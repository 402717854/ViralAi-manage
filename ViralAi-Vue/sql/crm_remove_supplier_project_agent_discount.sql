-- ----------------------------
-- 渠道产品明细下线「代理折扣 / 预计日消耗金额」
--
-- 背景：
--   crm_supplier_project 原有 min_agent_discount / max_agent_discount /
--   daily_usage_amount 三列，仅在「渠道管理 → 新增/修改供应商 → 产品明细」
--   与渠道详情抽屉中录入、展示，不参与任何计费或校验逻辑。
--   现已从产品明细中去掉，故一并清理表结构，避免留下永久为空的死列。
--
-- 本脚本内容：
--   1. 删除 crm_supplier_project.min_agent_discount；
--   2. 删除 crm_supplier_project.max_agent_discount；
--   3. 删除 crm_supplier_project.daily_usage_amount。
--
-- 说明：
--   · 成本折扣（min/max_cost_discount）与售价折扣（min/max_sell_discount）保留，
--     商机/合同明细仍以渠道折扣作为默认值来源。
--   · MySQL 5.7 不支持 drop column if exists，用存储过程判断 information_schema，
--     全部语句幂等，可重复执行。
--   · 删列不可逆，执行前建议备份：
--     create table crm_supplier_project_bak_20261119 as select * from crm_supplier_project;
-- ----------------------------
drop procedure if exists crm_drop_column_if_exists;
delimiter $$
create procedure crm_drop_column_if_exists(in p_table varchar(64), in p_column varchar(64))
begin
  if exists (select 1 from information_schema.columns
              where table_schema = database() and table_name = p_table and column_name = p_column) then
    set @crm_ddl = concat('alter table `', p_table, '` drop column `', p_column, '`');
    prepare crm_stmt from @crm_ddl;
    execute crm_stmt;
    deallocate prepare crm_stmt;
  end if;
end$$
delimiter ;

call crm_drop_column_if_exists('crm_supplier_project', 'min_agent_discount');
call crm_drop_column_if_exists('crm_supplier_project', 'max_agent_discount');
call crm_drop_column_if_exists('crm_supplier_project', 'daily_usage_amount');

drop procedure if exists crm_drop_column_if_exists;

-- 校验：应只剩 supplier_id、project_id、成本/售价折扣与审计字段
-- show columns from crm_supplier_project;
