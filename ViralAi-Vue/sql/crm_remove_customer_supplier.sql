-- ----------------------------
-- 客户关联渠道下线
--
-- 背景：
--   旧模型：客户资料维护「客户 1:N 渠道」（crm_customer_supplier），
--           商机/合同明细只能从客户已关联渠道中选择模型。
--   新模型：客户资料只维护客户自身属性；客户-渠道-模型关系改由商机明细
--           （crm_business_product）与合同明细（crm_contract_product）承载，
--           每行强制记录 supplier_id + product_id + supplier_account_id，
--           并在保存时由服务端做强制性校验：
--             1) 渠道必须存在、未删除且启用（crm_supplier.status = 1）；
--             2) 模型必须在渠道上架（crm_supplier_project）且模型有效；
--             3) 渠道账户必须属于该行所选渠道（crm_supplier_account.supplier_id）。
--
-- 本脚本内容：
--   1. 删除客户菜单下的「客户渠道维护」按钮权限（perms = crm:customer:supplier）；
--   2. 删除客户-渠道关联表 crm_customer_supplier；
--   3. 可选：删除 crm_business.supplier_id / crm_contract.supplier_id 废弃列
--            （默认注释，确认无历史审计诉求后再手工执行）。
--
-- 全部语句幂等，可重复执行。
-- ----------------------------

-- ----------------------------
-- 1、移除「客户渠道维护」菜单权限
--    先删角色授权关系（sys_role_menu），再删菜单，避免留下悬空授权。
-- ----------------------------
set @cs_menu_id = (select menu_id from sys_menu where perms = 'crm:customer:supplier' limit 1);

delete from sys_role_menu where menu_id = @cs_menu_id;

delete from sys_menu where menu_id = @cs_menu_id;

-- ----------------------------
-- 2、删除客户-渠道关联表
--    关系数据已由商机/合同明细承载，历史组合可通过
--    crm_business_product / crm_contract_product 反查。
-- ----------------------------
drop table if exists `crm_customer_supplier`;

-- ----------------------------
-- 3、可选清理：主表废弃渠道列
--    渠道已完全由明细行承载，主表这两列不再读写。保留可作历史审计；
--    如需彻底清理，确认无报表依赖后取消下面两行注释再执行。
-- ----------------------------
-- alter table crm_business drop column supplier_id;
-- alter table crm_contract drop column supplier_id;
