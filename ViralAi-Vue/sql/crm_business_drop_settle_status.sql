-- ----------------------------
-- 商机管理移除「是否结算」能力（配套脚本）
-- 变更内容：
--   1、备份 crm_business.settle_status 后删除该字段
--   2、删除「标记已结算」按钮权限 crm:business:settle 及其角色授权
--   3、删除商机结算状态字典 crm_bus_settle（含字典数据）
-- 影响范围：账单结算不再回写商机结算状态，结算能力统一收敛到「对账账单」模块
-- 数据库：MySQL 5.7+ / 8.0
-- 说明：脚本可重复执行（幂等），已删除的部分自动跳过
-- ----------------------------

-- ----------------------------
-- 1、备份待删除字段数据（仅首次执行、且字段存在时建表）
--    备注：备份表保留原始 0/1 值，便于回滚或事后核对；确认无需回滚后可手工删除
-- ----------------------------
set @has_settle_col = (select count(*) from information_schema.columns
                        where table_schema = database()
                          and table_name = 'crm_business'
                          and column_name = 'settle_status');

set @has_settle_bak = (select count(*) from information_schema.tables
                        where table_schema = database()
                          and table_name = 'crm_business_settle_bak');

set @ddl = if(@has_settle_col > 0 and @has_settle_bak = 0,
  'create table crm_business_settle_bak as select id, settle_status, update_time from crm_business',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、删除字段 settle_status
-- ----------------------------
set @ddl = if(@has_settle_col > 0,
  'alter table crm_business drop column settle_status',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 3、删除按钮权限 crm:business:settle
-- ----------------------------
delete from sys_role_menu
 where menu_id in (select menu_id from sys_menu where perms = 'crm:business:settle');

delete from sys_menu where perms = 'crm:business:settle';

-- ----------------------------
-- 4、删除字典 crm_bus_settle
-- ----------------------------
delete from sys_dict_data where dict_type = 'crm_bus_settle';
delete from sys_dict_type where dict_type = 'crm_bus_settle';

-- ----------------------------
-- 5、校验（应为 0 / 0 / 0）
-- ----------------------------
-- select count(*) as settle_col_cnt from information_schema.columns
--  where table_schema = database() and table_name = 'crm_business' and column_name = 'settle_status';
-- select count(*) as settle_menu_cnt from sys_menu where perms = 'crm:business:settle';
-- select count(*) as settle_dict_cnt from sys_dict_type where dict_type = 'crm_bus_settle';

-- ----------------------------
-- 回滚脚本（仅需回滚时手工执行）
-- ----------------------------
-- alter table crm_business add column settle_status tinyint(4) not null default 0
--   comment '结算状态0未结算1已结算' after contact_status;
-- update crm_business b inner join crm_business_settle_bak k on b.id = k.id
--    set b.settle_status = k.settle_status;
-- insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
-- select '商机结算状态', 'crm_bus_settle', '0', 'admin', sysdate(), '商机对账结算标记'
-- from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bus_settle');
-- insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
-- select 1, '未结算', '0', 'crm_bus_settle', '', 'info', 'Y', '0', 'admin', sysdate(), '默认值'
-- from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_settle' and dict_value = '0');
-- insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
-- select 2, '已结算', '1', 'crm_bus_settle', '', 'success', 'N', '0', 'admin', sysdate(), '对账完成后回填'
-- from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bus_settle' and dict_value = '1');
-- 按钮权限需按 crm_business_refactor.sql 第 5 节的菜单脚本重新插入（perms = 'crm:business:settle'）
