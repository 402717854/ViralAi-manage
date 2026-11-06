-- ----------------------------
-- 商机模块重构配套脚本（crm_business / crm_business_product / crm_business_log）
-- 业务约束：一条商机绑定唯一渠道，商机产品仅可选该渠道下已关联的模型；
--           成交后一键转合同，商机产品清单按快照复制为合同明细。
-- 数据库：MySQL 5.7+ / 8.0
-- 说明：本脚本可重复执行（幂等），建表用 create table if not exists，
--       加列/加索引用 information_schema 判断，字典与菜单用 not exists 判断。
-- ----------------------------

-- ----------------------------
-- 1、商机操作日志表
--    记录状态变更、产品清单维护、转合同、结算回填等关键动作，供详情页「操作日志」Tab 展示
-- ----------------------------
create table if not exists crm_business_log (
  id                  bigint(20)      not null auto_increment    comment '主键',
  bus_id              bigint(20)      not null                   comment '商机ID',
  action              varchar(32)     not null                   comment '操作类型 CREATE/UPDATE/STATUS/PRODUCT/CONVERT/SETTLE/DELETE',
  action_name         varchar(64)     default null               comment '操作名称',
  from_status         int(4)          default null               comment '变更前状态',
  to_status           int(4)          default null               comment '变更后状态',
  content             varchar(1000)   default null               comment '操作内容',
  operator_id         bigint(20)      default null               comment '操作人ID',
  operator_name       varchar(64)     default null               comment '操作人名称',
  create_time         datetime        default null               comment '操作时间',
  primary key (id),
  key idx_bl_bus (bus_id),
  key idx_bl_time (bus_id, create_time)
) engine=innodb auto_increment=1 comment = '商机操作日志表';

-- ----------------------------
-- 2、商机产品表补列（价格明细）
--    与 crm_contract_product 字段完全对齐，保证转合同可直接复制，无需映射转换
-- ----------------------------
set @has_price_detail = (select count(*) from information_schema.columns
                          where table_schema = database()
                            and table_name = 'crm_business_product'
                            and column_name = 'price_detail');

set @ddl = if(@has_price_detail = 0,
  'alter table crm_business_product add column price_detail varchar(2000) default null comment ''价格明细 JSON'' after sell_discount',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 3、商机表索引（按文档第七章补充，查询已存在时跳过）
-- ----------------------------
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business' and index_name = 'idx_customer_id');
set @ddl = if(@idx = 0, 'alter table crm_business add index idx_customer_id (customer_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business' and index_name = 'idx_supplier_id');
set @ddl = if(@idx = 0, 'alter table crm_business add index idx_supplier_id (supplier_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business' and index_name = 'idx_bus_status');
set @ddl = if(@idx = 0, 'alter table crm_business add index idx_bus_status (bus_status)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business' and index_name = 'idx_owner_user');
set @ddl = if(@idx = 0, 'alter table crm_business add index idx_owner_user (owner_user)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business' and index_name = 'idx_bus_deal_time');
set @ddl = if(@idx = 0, 'alter table crm_business add index idx_bus_deal_time (deal_time)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、字典（已存在的站点不重复插入，可安全覆盖 list_class 以匹配前端标签色）
-- ----------------------------
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '商机状态', 'crm_bus_status', '0', 'admin', sysdate(), '商机状态列表'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bus_status');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '初始化', '0', 'crm_bus_status', '', 'info', 'Y', '0', 'admin', sysdate(), '新建商机默认状态'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '跟进中', '1', 'crm_bus_status', '', 'primary', 'N', '0', 'admin', sysdate(), '可维护产品清单'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '提交测试', '2', 'crm_bus_status', '', 'primary', 'N', '0', 'admin', sysdate(), '产品清单自此锁定'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '测试中', '3', 'crm_bus_status', '', 'warning', 'N', '0', 'admin', sysdate(), '测试通过可成交'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '3');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '成交', '4', 'crm_bus_status', '', 'success', 'N', '0', 'admin', sysdate(), '可一键转合同'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '4');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 6, '暂停', '5', 'crm_bus_status', '', 'warning', 'N', '0', 'admin', sysdate(), '可恢复跟进'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '5');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 7, '中断', '6', 'crm_bus_status', '', 'danger', 'N', '0', 'admin', sysdate(), '终态，不可恢复'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_status' and dict_value = '6');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '商机合同状态', 'crm_bus_contact', '0', 'admin', sysdate(), '商机关联合同签署状态'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bus_contact');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '未签', '0', 'crm_bus_contact', '', 'info', 'Y', '0', 'admin', sysdate(), '未生成签约合同'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_contact' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '已签', '1', 'crm_bus_contact', '', 'success', 'N', '0', 'admin', sysdate(), '合同审批通过后自动回写'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_contact' and dict_value = '1');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '商机结算状态', 'crm_bus_settle', '0', 'admin', sysdate(), '商机对账结算标记'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bus_settle');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '未结算', '0', 'crm_bus_settle', '', 'info', 'Y', '0', 'admin', sysdate(), '默认值'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_settle' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '已结算', '1', 'crm_bus_settle', '', 'success', 'N', '0', 'admin', sysdate(), '对账完成后回填'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_settle' and dict_value = '1');

-- ----------------------------
-- 5、菜单与按钮权限（幂等）
--    说明：RuoYi 仅为目录型菜单递归生成子路由，隐藏页必须挂在目录（M）下才会生成路由，
--          因此「商机编辑 / 商机详情」直接挂到 CRM 一级目录。
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

-- 5.1 商机管理菜单（已存在则不重复创建）
set @business_menu = (select menu_id from sys_menu where perms = 'crm:business:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '商机管理', @crm_parent, 3, 'business', 'crm/business/index', '', '',
       1, 0, 'C', '0', '0', 'crm:business:list', 'skill', 'admin', sysdate(), '', null, '商机管理菜单'
from dual where @business_menu is null;

set @business_menu = (select menu_id from sys_menu where perms = 'crm:business:list' limit 1);

-- 5.2 编辑页（隐藏路由）
set @edit_menu = (select menu_id from sys_menu where perms = 'crm:business:edit-page' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '商机编辑', @crm_parent, 3, 'business/edit', 'crm/business/edit', '', '',
       1, 0, 'C', '1', '0', 'crm:business:edit-page', '#', 'admin', sysdate(), '', null, '商机新增/编辑页，隐藏路由'
from dual where @edit_menu is null;

-- 5.3 详情页（隐藏路由）
set @detail_menu = (select menu_id from sys_menu where perms = 'crm:business:view-page' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '商机详情', @crm_parent, 3, 'business/detail', 'crm/business/view', '', '',
       1, 0, 'C', '1', '0', 'crm:business:view-page', '#', 'admin', sysdate(), '', null, '商机详情页，隐藏路由'
from dual where @detail_menu is null;

-- 5.4 按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机新增', @business_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:add', '#', 'admin', sysdate(), '', null, '新建商机'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:add');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机修改', @business_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:edit', '#', 'admin', sysdate(), '', null, '编辑商机与状态流转'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:edit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机删除', @business_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:remove', '#', 'admin', sysdate(), '', null, '仅初始化/跟进中可删'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:remove');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机详情查询', @business_menu, 4, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:query', '#', 'admin', sysdate(), '', null, '商机详情与明细查看'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:query');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机导出', @business_menu, 5, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:export', '#', 'admin', sysdate(), '', null, '导出商机列表'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:export');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机产品维护', @business_menu, 6, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:product', '#', 'admin', sysdate(), '', null, '仅初始化/跟进中可维护产品清单'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:product');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机转合同', @business_menu, 7, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:convert', '#', 'admin', sysdate(), '', null, '成交商机一键生成草稿合同'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:convert');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机结算回填', @business_menu, 8, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:settle', '#', 'admin', sysdate(), '', null, '运营/管理回填结算状态'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:settle');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '商机全量查看', @business_menu, 9, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:business:all', '#', 'admin', sysdate(), '', null, '不受渠道/创建人数据权限限制'
from dual where @business_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:business:all');

-- ----------------------------
-- 6、校验
-- ----------------------------
-- select menu_id, menu_name, parent_id, menu_type, visible, perms from sys_menu where perms like 'crm:business%';
-- select dict_type, dict_label, dict_value from sys_dict_data where dict_type like 'crm_bus%' order by dict_type, dict_sort;
-- show create table crm_business_product;
