-- ----------------------------
-- 合同管理模块（crm_contract / crm_contract_product / crm_contract_log / sys_no_generator）
-- 业务约束：一份合同归属单一渠道，合同明细仅可选择该渠道下已关联的大模型产品
-- 数据库：MySQL 5.7+ / 8.0
-- 执行顺序：1 建表 → 2 供应商渠道编码 → 3 字典 → 4 菜单 → 5 定时任务
-- 说明：本脚本可重复执行（幂等），建表使用 create table if not exists，
--       加列/加索引使用 information_schema 判断，菜单与字典使用 not exists 判断。
-- ----------------------------

-- ----------------------------
-- 1、合同主表
-- ----------------------------
create table if not exists crm_contract (
  id                  bigint(20)      not null auto_increment    comment '主键',
  bus_id              bigint(20)      default null               comment '商机ID',
  consumer_id         bigint(20)      not null                   comment '客户ID（crm_customer.id）',
  supplier_id         bigint(20)      not null                   comment '渠道ID（crm_supplier.id）',
  contact_no          varchar(64)     not null                   comment '合同编号 HT-{渠道编码}-{yyyyMMdd}-{6位序列}',
  contract_name       varchar(200)    not null                   comment '合同名称',
  status              int(4)          not null default 0         comment '合同状态：0草稿 1审批中 2已签约 3已到期 4已作废 5已归档',
  contract_entity     varchar(200)    default null               comment '签约主体',
  expire_time         datetime        default null               comment '合同到期时间',
  audit_time          datetime        default null               comment '审批时间',
  sign_time           datetime        default null               comment '签约时间',
  cancel_time         datetime        default null               comment '作废时间',
  archive_time        datetime        default null               comment '归档时间',
  audit_opinion       varchar(500)    default null               comment '审批意见',
  cancel_reason       varchar(500)    default null               comment '作废原因',
  remark              varchar(500)    default null               comment '备注',
  creator             bigint(20)      default null               comment '创建者',
  create_time         datetime        default null               comment '创建时间',
  updater             bigint(20)      default null               comment '更新者',
  update_time         datetime        default null               comment '更新时间',
  deleted             tinyint(1)      default 0                  comment '是否删除 0否 1是',
  primary key (id),
  unique key uk_contract_contact_no (contact_no),
  key idx_contract_supplier (supplier_id, deleted),
  key idx_contract_consumer (consumer_id, deleted),
  key idx_contract_status (status, deleted),
  key idx_contract_creator (creator, deleted),
  key idx_contract_expire (status, expire_time)
) engine=innodb auto_increment=1 comment = '合同主表';

-- ----------------------------
-- 2、合同模型产品明细表
--    uk_contract_product 作为「同一合同不可重复同一模型」的兜底约束
-- ----------------------------
create table if not exists crm_contract_product (
  id                  bigint(20)      not null auto_increment    comment '主键',
  contract_id         bigint(20)      not null                   comment '合同ID',
  supplier_id         bigint(20)      not null                   comment '渠道ID（冗余自合同主表，用于归属强校验）',
  product_id          bigint(20)      not null                   comment '模型产品ID（crm_model_product.id）',
  cost_discount       decimal(10,2)   default null               comment '成本折扣 0.01~1.00',
  sell_discount       decimal(10,2)   default null               comment '售卖折扣 0.01~1.00',
  price_detail        varchar(2000)   default null               comment '价格明细 JSON',
  remark              varchar(500)    default null               comment '备注',
  creator             bigint(20)      default null               comment '创建者',
  create_time         datetime        default null               comment '创建时间',
  updater             bigint(20)      default null               comment '更新者',
  update_time         datetime        default null               comment '更新时间',
  deleted             tinyint(1)      default 0                  comment '是否删除 0否 1是',
  primary key (id),
  unique key uk_contract_product (contract_id, product_id),
  key idx_cp_contract (contract_id, deleted),
  key idx_cp_product (product_id, deleted)
) engine=innodb auto_increment=1 comment = '合同模型产品明细表';

-- ----------------------------
-- 3、合同操作日志表（同时承载审批记录）
-- ----------------------------
create table if not exists crm_contract_log (
  id                  bigint(20)      not null auto_increment    comment '主键',
  contract_id         bigint(20)      not null                   comment '合同ID',
  action              varchar(32)     not null                   comment '操作类型 CREATE/UPDATE/SUBMIT/AUDIT_PASS/AUDIT_REJECT/CANCEL/ARCHIVE/DELETE/EXPIRE',
  action_name         varchar(64)     default null               comment '操作名称',
  from_status         int(4)          default null               comment '变更前状态',
  to_status           int(4)          default null               comment '变更后状态',
  content             varchar(1000)   default null               comment '操作内容/审批意见/作废原因',
  operator_id         bigint(20)      default null               comment '操作人ID',
  operator_name       varchar(64)     default null               comment '操作人名称',
  create_time         datetime        default null               comment '操作时间',
  primary key (id),
  key idx_cl_contract (contract_id, id)
) engine=innodb auto_increment=1 comment = '合同操作日志表';

-- ----------------------------
-- 4、合同编号生成器（方案 A：数据库号段）
--    按 biz_type + supplier_code + gen_date 分组自增，编号一旦生成永不修改
-- ----------------------------
create table if not exists sys_no_generator (
  id                  bigint(20)      not null auto_increment    comment '主键',
  biz_type            varchar(32)     not null                   comment '业务类型 contract',
  supplier_code       varchar(64)     not null                   comment '渠道编码',
  gen_date            varchar(8)      not null                   comment '生成日期 yyyyMMdd',
  last_seq            int(11)         not null default 0         comment '当前已分配序列',
  primary key (id),
  unique key uk_biz_supplier_date (biz_type, supplier_code, gen_date)
) engine=innodb auto_increment=1 comment = '业务编号生成器';

-- ----------------------------
-- 5、渠道编码（crm_supplier.supplier_code）
--    合同编号规则依赖渠道编码，缺失时按 QD + 3 位主键补全
-- ----------------------------
set @col_exists = (select count(1) from information_schema.columns
                    where table_schema = database() and table_name = 'crm_supplier' and column_name = 'supplier_code');
set @ddl = if(@col_exists = 0,
  'alter table crm_supplier add column supplier_code varchar(64) default null comment ''渠道编码，用于合同编号'' after id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

update crm_supplier
   set supplier_code = concat('QD', lpad(id, 3, '0'))
 where supplier_code is null or supplier_code = '';

set @idx_exists = (select count(1) from information_schema.statistics
                    where table_schema = database() and table_name = 'crm_supplier' and index_name = 'uk_supplier_code');
set @ddl = if(@idx_exists = 0,
  'alter table crm_supplier add unique key uk_supplier_code (supplier_code)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 6、字典数据（幂等）
--    前端用法：const { crm_contract_status, crm_audit_result } = proxy.useDict('crm_contract_status', 'crm_audit_result')
-- ----------------------------
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '合同状态', 'crm_contract_status', '0', 'admin', sysdate(), '合同管理-合同状态'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_contract_status');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '合同审批结果', 'crm_audit_result', '0', 'admin', sysdate(), '合同管理-审批结果'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_audit_result');

-- 6.1 合同状态：草稿灰、审批中蓝、已签约绿、已到期橙、已作废红、已归档深灰
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '草稿', '0', 'crm_contract_status', '', 'info', 'Y', '0', 'admin', sysdate(), '可编辑，可提交审批'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '审批中', '1', 'crm_contract_status', '', 'primary', 'N', '0', 'admin', sysdate(), '锁定编辑，可审批/撤回'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '已签约', '2', 'crm_contract_status', '', 'success', 'N', '0', 'admin', sysdate(), '可作废、归档'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '已到期', '3', 'crm_contract_status', '', 'warning', 'N', '0', 'admin', sysdate(), '定时任务自动变更，可归档'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '3');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '已作废', '4', 'crm_contract_status', '', 'danger', 'N', '0', 'admin', sysdate(), '终态，可归档'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '4');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 6, '已归档', '5', 'crm_contract_status', '', 'info', 'N', '0', 'admin', sysdate(), '终态，只读'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_contract_status' and dict_value = '5');

-- 6.2 审批结果
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '通过', '1', 'crm_audit_result', '', 'success', 'Y', '0', 'admin', sysdate(), '审批通过并签约'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_audit_result' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '驳回', '2', 'crm_audit_result', '', 'danger', 'N', '0', 'admin', sysdate(), '退回草稿重新编辑'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_audit_result' and dict_value = '2');

-- ----------------------------
-- 7、菜单与按钮权限（幂等）
--    父菜单：优先取 path='crm' 的一级目录，其次取名称含 CRM 的一级目录；
--    仍找不到时挂到根节点，此时前端路由为 /contract，需按实际调整。
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

-- 7.1 合同管理菜单
set @contract_menu = (select menu_id from sys_menu where perms = 'crm:contract:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '合同管理', @crm_parent, 5, 'contract', 'crm/contract/index', '', '',
       1, 0, 'C', '0', '0', 'crm:contract:list', 'documentation', 'admin', sysdate(), '', null, '合同管理菜单'
from dual where @contract_menu is null;

set @contract_menu = (select menu_id from sys_menu where perms = 'crm:contract:list' limit 1);

-- 7.2 新增/编辑页（隐藏菜单，仅用于路由）
--    可见性设为隐藏（visible='1'），且必须挂在目录型菜单下，RuoYi 才会为其生成路由
set @edit_menu = (select menu_id from sys_menu where perms = 'crm:contract:edit-page' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '合同编辑', @crm_parent, 6, 'contract/edit', 'crm/contract/edit', '', '',
       1, 0, 'C', '1', '0', 'crm:contract:edit-page', '#', 'admin', sysdate(), '', null, '合同新增/编辑页，隐藏路由'
from dual where @edit_menu is null;

-- 7.3 详情页（隐藏菜单，仅用于路由）
set @view_menu = (select menu_id from sys_menu where perms = 'crm:contract:view-page' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '合同详情', @crm_parent, 7, 'contract/detail', 'crm/contract/view', '', '',
       1, 0, 'C', '1', '0', 'crm:contract:view-page', '#', 'admin', sysdate(), '', null, '合同详情页，隐藏路由'
from dual where @view_menu is null;

-- 7.4 按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同新增', @contract_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:add', '#', 'admin', sysdate(), '', null, '合同新建保存'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:add');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同修改', @contract_menu, 4, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:edit', '#', 'admin', sysdate(), '', null, '合同草稿编辑'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:edit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同删除', @contract_menu, 5, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:remove', '#', 'admin', sysdate(), '', null, '删除草稿合同'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:remove');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同提交审批', @contract_menu, 6, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:submit', '#', 'admin', sysdate(), '', null, '草稿提交审批'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:submit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同审批', @contract_menu, 7, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:audit', '#', 'admin', sysdate(), '', null, '审批通过/驳回'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:audit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同作废', @contract_menu, 8, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:cancel', '#', 'admin', sysdate(), '', null, '已签约/已到期合同作废'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:cancel');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同归档', @contract_menu, 9, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:archive', '#', 'admin', sysdate(), '', null, '合同归档'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:archive');

-- 7.5 详情查询权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同详情查询', @contract_menu, 10, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:query', '#', 'admin', sysdate(), '', null, '合同详情与明细查看'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:query');

-- 7.6 全量查看权限（数据权限开关）：拥有该权限的账号可查看全部渠道合同
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '合同全量查看', @contract_menu, 11, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:contract:all', '#', 'admin', sysdate(), '', null, '不受渠道/创建人数据权限限制'
from dual where @contract_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:contract:all');

-- ----------------------------
-- 8、到期自动任务（每天凌晨 1 点执行）
--    invoke_target 对应 CrmContractExpireTask 的 Spring Bean 名与方法名
-- ----------------------------
insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
                     status, create_by, create_time, remark)
select '合同到期自动变更', 'SYSTEM', 'crmContractTask.expireContracts()', '0 0 1 * * ?', '3', '1',
       '0', 'admin', sysdate(), '每天凌晨将已签约且已过到期时间的合同置为已到期'
from dual where not exists (select 1 from sys_job where invoke_target = 'crmContractTask.expireContracts()');

-- ----------------------------
-- 9、校验
-- ----------------------------
-- select contact_no, contract_name, status from crm_contract order by id desc limit 10;
-- select biz_type, supplier_code, gen_date, last_seq from sys_no_generator;
-- select menu_id, menu_name, parent_id, menu_type, perms from sys_menu where perms like 'crm:contract%';
