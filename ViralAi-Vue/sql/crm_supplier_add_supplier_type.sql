-- ----------------------------
-- 渠道管理（crm_supplier）：新增「渠道类型」supplier_type
--   背景：渠道需要区分「自营」与「三方」，用于筛选与对账口径区分。
--   枚举：1 自营、2 三方（与前端 views/crm/supplier 的 supplierTypeOptions 一致，
--         后端 CrmSupplierDto 通过 @Excel 的 readConverterExp 做导出翻译）。
--   默认值：存量渠道一律视为自营（1），与新增弹窗的默认值保持一致。
-- 数据库：MySQL 5.7+ / 8.0
-- 幂等：可重复执行
-- ----------------------------

-- 删列不可逆，执行前建议备份：
-- create table crm_supplier_bak_20261120 as select * from crm_supplier;

-- ----------------------------
-- 1、新增列 supplier_type
-- ----------------------------
set @has_column = (
  select count(*) from information_schema.columns
   where table_schema = database()
     and table_name = 'crm_supplier'
     and column_name = 'supplier_type'
);
set @ddl = if(@has_column = 0,
  'alter table crm_supplier add column supplier_type tinyint(1) not null default 1 comment ''渠道类型：1自营 2三方'' after name',
  'select ''column supplier_type already exists'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、回填历史数据：存量渠道按自营处理
--    仅处理空值，已明确标注为三方的数据不会被覆盖。
-- ----------------------------
update crm_supplier
   set supplier_type = 1
 where supplier_type is null;

-- ----------------------------
-- 3、索引补充：列表按渠道类型筛选
-- ----------------------------
set @has_index = (
  select count(*) from information_schema.statistics
   where table_schema = database()
     and table_name = 'crm_supplier'
     and index_name = 'idx_supplier_type'
);
set @ddl = if(@has_index = 0,
  'alter table crm_supplier add key idx_supplier_type (supplier_type)',
  'select ''index idx_supplier_type already exists'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、字典（可选）：crm_supplier_type
--    前端当前使用本地常量渲染，此处写入字典便于在「系统管理-字典管理」统一维护口径。
--    按 dict_type + dict_value 判重，已存在则跳过。
-- ----------------------------
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '渠道类型', 'crm_supplier_type', '0', 'admin', sysdate(), 'CRM 渠道类型：自营/三方'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_supplier_type');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '自营', '1', 'crm_supplier_type', '', 'success', 'Y', '0', 'admin', sysdate(), '自营渠道'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_supplier_type' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '三方', '2', 'crm_supplier_type', '', 'warning', 'N', '0', 'admin', sysdate(), '三方渠道'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_supplier_type' and dict_value = '2');
