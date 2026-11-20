-- ----------------------------
-- 模型产品下线「基准售价 / 基准成本价」
--
-- 背景：
--   模型产品（crm_model_product）原有 base_sell_price / base_cost_price 两列，
--   早期用于「token 用量 × 基准价 × 合同折扣」计费。
--   现行计费口径已改为按上游费用（total_fees / actual_fees）× 合同折扣计算
--   （见 CrmUsageMatchEngine#match），基准价不再参与任何计算，
--   仅在新增/编辑表单与列表中冗余维护，属无效字段。
--
-- 本脚本内容：
--   1. 删除 crm_model_product.base_sell_price；
--   2. 删除 crm_model_product.base_cost_price。
--
-- 说明：
--   · crm_contract_usage（用量台账）中的 base_sell_price / base_cost_price 是
--     计费时的快照列，属台账表而非模型表，本脚本不动；历史台账数据保持可查。
--   · MySQL 5.7 不支持 drop column if exists，用存储过程判断 information_schema，
--     全部语句幂等，可重复执行。
--   · 执行前建议备份：create table crm_model_product_bak_20261119 as select * from crm_model_product;
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

call crm_drop_column_if_exists('crm_model_product', 'base_sell_price');
call crm_drop_column_if_exists('crm_model_product', 'base_cost_price');

drop procedure if exists crm_drop_column_if_exists;

-- 校验：应只剩 product_code、price_detail 等字段，不再出现 base_sell_price / base_cost_price
-- show columns from crm_model_product;
