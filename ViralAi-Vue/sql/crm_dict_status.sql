-- ----------------------------
-- CRM 通用字典补齐：crm_status、crm_deleted
--   问题：sys_dict_type / sys_dict_data 中从未维护这两个字典，
--         前端 <dict-tag> 取不到选项时会退化为直接显示原始数字（0/1/2），
--         表单里的状态下拉也会变成空列表。
--   影响页面：客户管理、渠道管理、渠道账户、模型产品等所有引用这两个字典的位置。
--   说明：渠道管理（views/crm/supplier）已改用本地常量 views/crm/supplier/constants.js，
--         不依赖本字典；本脚本用于其余仍走字典的页面，执行后需刷新页面或清理字典缓存。
--   幂等：按 dict_type + dict_value 判断，已存在的记录跳过，可重复执行。
-- 数据库：MySQL 5.7+ / 8.0
-- ----------------------------

-- ----------------------------
-- 1、通用状态：0 未启用、1 启用、2 暂停
-- ----------------------------
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '通用状态', 'crm_status', '0', 'admin', sysdate(), 'CRM 渠道、客户、模型等启用状态'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_status');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '未启用', '0', 'crm_status', '', 'info', 'N', '0', 'admin', sysdate(), '未启用，不可用于商机/合同明细'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_status' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '启用', '1', 'crm_status', '', 'success', 'Y', '0', 'admin', sysdate(), '启用，可作为明细渠道来源'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_status' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '暂停', '2', 'crm_status', '', 'warning', 'N', '0', 'admin', sysdate(), '暂停，暂不参与新明细选择'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_status' and dict_value = '2');

-- ----------------------------
-- 2、逻辑删除标记：0 正常、1 已删除
-- ----------------------------
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '是否删除', 'crm_deleted', '0', 'admin', sysdate(), 'CRM 逻辑删除标记'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_deleted');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '正常', '0', 'crm_deleted', '', 'success', 'Y', '0', 'admin', sysdate(), '未删除'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_deleted' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '已删除', '1', 'crm_deleted', '', 'danger', 'N', '0', 'admin', sysdate(), '已逻辑删除'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_deleted' and dict_value = '1');
