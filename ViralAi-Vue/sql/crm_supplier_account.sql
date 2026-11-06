-- ----------------------------
-- 渠道账户管理（crm_supplier_account）
-- 说明：
--   1. 表结构以业务方提供的 DDL 为准，脚本用 if not exists 保证可在新环境重复执行；
--   2. 菜单与按钮权限全部用 not exists 判断，重复执行不会产生重复数据；
--   3. 父菜单取顶级 CRM 目录，取不到时挂到根节点。
-- ----------------------------

-- ----------------------------
-- 1、渠道账户表
-- ----------------------------
create table if not exists `crm_supplier_account` (
  `id`           bigint       not null auto_increment,
  `account_name` varchar(100) not null comment '账户名',
  `account_pwd`  varchar(255) default null comment '账户密码',
  `api_key_no`   varchar(100) default null comment 'apiKey编号',
  `api_key`      varchar(300) not null comment 'apiKey',
  `supplier_id`  bigint       not null comment '供应商关联ID',
  `status`       int          default null comment '状态0未启用1启用',
  `type`         int          not null comment '账户类型1测试账户2真实账户',
  `create_time`  datetime     not null,
  `update_time`  datetime     default null,
  `creator`      int          not null comment '创建者ID',
  `updater`      int          default null comment '修改者ID',
  primary key (`id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_general_ci comment='渠道账户表';

-- 按渠道过滤账户的索引（已存在则跳过）
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'crm_supplier_account'
               and index_name = 'idx_sa_supplier');
set @ddl = if(@idx = 0, 'alter table crm_supplier_account add index idx_sa_supplier (supplier_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、菜单与按钮权限（幂等）
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

-- 2.1 渠道账户管理菜单
set @acc_menu = (select menu_id from sys_menu where perms = 'crm:account:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '渠道账户管理', @crm_parent, 8, 'account', 'crm/supplierAccount/index', '', '',
       1, 0, 'C', '0', '0', 'crm:account:list', 'money', 'admin', sysdate(), '', null, '渠道账户管理菜单'
from dual where @acc_menu is null;

set @acc_menu = (select menu_id from sys_menu where perms = 'crm:account:list' limit 1);

-- 2.2 按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '渠道账户新增', @acc_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:account:add', '#', 'admin', sysdate(), '', null, '新增渠道账户'
from dual where @acc_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:account:add');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '渠道账户修改', @acc_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:account:edit', '#', 'admin', sysdate(), '', null, '修改渠道账户'
from dual where @acc_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:account:edit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '渠道账户详情', @acc_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:account:query', '#', 'admin', sysdate(), '', null, '查看渠道账户详情（含完整 apiKey）'
from dual where @acc_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:account:query');
