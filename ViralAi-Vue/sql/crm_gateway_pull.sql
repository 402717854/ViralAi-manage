-- ============================================================
-- 网关流水拉取（按渠道 + 渠道账户）
--
-- 背景：
--   拉取能力此前是「一个全局地址 + 一个全局令牌」，所有渠道共用，实际无法使用
--   （application.yml 里 crm.gateway.pull-url 为空，pullAndIngest 一律抛业务异常）。
--   而两个真实上游的接口形态完全不同：
--     · viralai 中转站（new-api 系）：GET  /api/log/，Bearer 令牌，页码分页，
--       记录自带 username（客户）与 model_name（模型），token 计量；
--     · pixverse：POST /openapi/v2/account/billing/usage-detail，API-KEY 头，
--       游标分页，单次区间 ≤30 天，记录**没有客户维度**，credits 计量。
--
-- 因此拉取配置下沉到「渠道账户」：一个账户 = 一套密钥 + 一种上游协议。
--
-- 本脚本幂等，可重复执行；MySQL 5.7+ / 8.0
-- ============================================================

-- ----------------------------
-- 1、渠道账户表补充拉取配置
--    pull_channel：适配器标识，代码里按此选择实现；为空时退回旧的通用 HTTP 拉取
--    pull_url    ：接口地址。pixverse 有 .ai / .cn 两套域名，中转站可自建，
--                  所以地址必须可覆盖，不能写死在代码里
--    pull_user   ：上游用户名过滤。中转站的 /api/log/ 支持 username 参数，
--                  留空表示不按用户过滤、拉取全量（管理员令牌的常见用法）
-- ----------------------------
set @c = (select count(*) from information_schema.columns
           where table_schema = database() and table_name = 'crm_supplier_account'
             and column_name = 'pull_channel');
set @ddl = if(@c = 0,
  'alter table crm_supplier_account add column pull_channel varchar(32) default null comment ''流水拉取适配器 viralaiai/pixverse，空=通用'' after type',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @c = (select count(*) from information_schema.columns
           where table_schema = database() and table_name = 'crm_supplier_account'
             and column_name = 'pull_url');
set @ddl = if(@c = 0,
  'alter table crm_supplier_account add column pull_url varchar(255) default null comment ''流水拉取接口地址，空=适配器默认地址'' after pull_channel',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @c = (select count(*) from information_schema.columns
           where table_schema = database() and table_name = 'crm_supplier_account'
             and column_name = 'pull_user');
set @ddl = if(@c = 0,
  'alter table crm_supplier_account add column pull_user varchar(100) default null comment ''流水拉取的用户名过滤，空=不按用户过滤'' after pull_url',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、上游标识映射表
--
--   上游给的是「它自己的标识」，本系统要的是「自己的主键」，两者必须有一张对照表：
--     · 客户：中转站记录带 username（如 Mark），pixverse 记录**完全没有客户字段**，
--       只能整账户归属一个客户。两种情况都能用 (账户 + username) 表达，用空串表示
--       「该账户的默认客户」。
--     · 模型：中转站给 model_name，pixverse 给 model(v6) + quality(720p) + creation_type，
--       都要映射到 crm_model_product.product_code 才能参与对账匹配。
--
--   匹配优先级（同一 map_type 内）：账户级精确 → 渠道级精确 → 账户级忽略变体 → 渠道级忽略变体。
--   未命中时：客户侧记为「未映射」不入库（避免把脏客户挂到流水上）；
--             模型侧退化为上游标识本身，由对账引擎判为「模型不在合同内」。
-- ----------------------------
create table if not exists `crm_gateway_pull_map` (
  `id`                  bigint       not null auto_increment                 comment '主键',
  `supplier_id`         bigint       not null                                comment '渠道ID',
  `supplier_account_id` bigint       not null default 0                       comment '渠道账户ID，0=该渠道通用',
  `map_type`            varchar(16)  not null                                comment '映射类型 CUSTOMER/PRODUCT',
  `upstream_key`        varchar(128) not null default ''                     comment 'CUSTOMER=上游用户名（空串表示账户默认客户）；PRODUCT=上游模型标识',
  `upstream_variant`    varchar(128) not null default ''                     comment '上游变体，如 pixverse 的 720p|text_to_video；无变体为空串',
  `target_id`           bigint       default null                            comment 'map_type=CUSTOMER 时为 crm_customer.id，其余为空',
  `target_code`         varchar(128) default null                            comment 'map_type=PRODUCT 时为 crm_model_product.product_code，其余为空',
  `status`              tinyint      not null default 1                      comment '状态 0失效 1有效',
  `remark`              varchar(255) default null                            comment '备注',
  `creator`             bigint       default null                            comment '创建者',
  `create_time`         datetime     default null                            comment '创建时间',
  `update_time`         datetime     default null                            comment '更新时间',
  primary key (`id`),
  unique key `uk_pull_map` (`supplier_id`, `supplier_account_id`, `map_type`, `upstream_key`, `upstream_variant`),
  key `idx_pull_map_account` (`supplier_account_id`, `map_type`, `status`)
) engine=innodb auto_increment=1 comment='网关流水上游标识映射';

-- ----------------------------
-- 3、菜单与权限（挂在「渠道账户管理」下，用于维护映射）
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

set @map_menu = (select menu_id from sys_menu where perms = 'crm:pullMap:list' limit 1);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '流水映射配置', @crm_parent, 9,
       'pullMap', 'crm/pullMap/index', '', '',
       1, 0, 'C', '0', '0', 'crm:pullMap:list', 'tree-table', 'admin', sysdate(), '', null,
       '网关流水上游客户/模型映射'
from dual where @map_menu is null;

set @map_menu = (select menu_id from sys_menu where perms = 'crm:pullMap:list' limit 1);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '映射新增', @map_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:pullMap:add', '#', 'admin', sysdate(), '', null, '新增映射'
from dual where @map_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:pullMap:add');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '映射修改', @map_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:pullMap:edit', '#', 'admin', sysdate(), '', null, '修改映射'
from dual where @map_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:pullMap:edit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '映射删除', @map_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:pullMap:remove', '#', 'admin', sysdate(), '', null, '删除映射'
from dual where @map_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:pullMap:remove');

-- ----------------------------
-- 4、配置样例（按需手工执行，勿直接跑）
--
--   4.1 viralai 中转站：一个账户对应中转站的一个管理员令牌，
--       客户的 username 逐个映射到 CRM 客户
--   update crm_supplier_account
--      set pull_channel = 'viralaiai',
--          pull_url     = 'https://viralaiai.com/api/log/',
--          pull_user    = 'Mark'
--    where id = {账户ID};
--
--   insert into crm_gateway_pull_map (supplier_id, supplier_account_id, map_type, upstream_key, upstream_variant, target_id, target_code, status, creator, create_time)
--   values
--     ({渠道ID}, 0, 'CUSTOMER', 'Mark',    '', {crm_customer.id}, null, 1, 1, sysdate()),
--     ({渠道ID}, 0, 'PRODUCT',  'gpt-4o',  '', null, 'gpt-4o',            1, 1, sysdate());
--
--   4.2 pixverse：记录没有客户维度，用 upstream_key = '' 表示「该账户的默认客户」，
--       模型按 型号|清晰度|能力 组合映射（pixverse 有 .ai 与 .cn 两套域名，按地域选）
--   update crm_supplier_account
--      set pull_channel = 'pixverse',
--          pull_url     = 'https://app-api.pixverse.ai/openapi/v2/account/billing/usage-detail'
--    where id = {账户ID};
--
--   insert into crm_gateway_pull_map (supplier_id, supplier_account_id, map_type, upstream_key, upstream_variant, target_id, target_code, status, creator, create_time)
--   values
--     ({渠道ID}, {账户ID}, 'CUSTOMER', '',   '',                     {crm_customer.id}, null, 1, 1, sysdate()),
--     ({渠道ID}, {账户ID}, 'PRODUCT',  'v6', '720p|text_to_video',   null, 'pixverse-v6-720p-t2v', 1, 1, sysdate());
-- ----------------------------

-- ----------------------------
-- 5、自查
-- ----------------------------
-- 5.1 已配置拉取协议的账户
select a.id, a.account_name, a.supplier_id, s.name as supplier_name,
       a.pull_channel, a.pull_url, a.pull_user, a.status,
       (select count(1) from crm_gateway_pull_map m
         where m.supplier_account_id = a.id and m.map_type = 'CUSTOMER' and m.status = 1) as customer_map_cnt,
       (select count(1) from crm_gateway_pull_map m
         where m.supplier_account_id = a.id and m.map_type = 'PRODUCT' and m.status = 1) as product_map_cnt
from crm_supplier_account a
left join crm_supplier s on s.id = a.supplier_id
order by a.supplier_id, a.id;

-- 5.2 已启用但未配拉取协议的账户（这些账户不会被定时任务拉取）
select a.id, a.account_name, a.supplier_id, s.name as supplier_name
from crm_supplier_account a
left join crm_supplier s on s.id = a.supplier_id
where a.status = 1
  and (a.pull_channel is null or a.pull_channel = '')
order by a.supplier_id, a.id;

-- 5.3 映射指向的目标是否仍然有效（应返回 0 行）
select m.id, m.map_type, m.upstream_key, m.target_id, m.target_code
from crm_gateway_pull_map m
where m.status = 1
  and ((m.map_type = 'CUSTOMER' and not exists (select 1 from crm_customer c where c.id = m.target_id))
    or (m.map_type = 'PRODUCT'  and not exists (select 1 from crm_model_product p
                                                 where p.product_code = m.target_code and p.deleted = 0)));
