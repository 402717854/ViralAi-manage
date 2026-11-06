-- ----------------------------
-- 商机/合同明细增加「渠道账户」字段（supplier_account_id）
--
-- 背景：
--   渠道由明细行承载后，明细除了要指定渠道（supplier_id），还需要指定该渠道下
--   实际使用的账户（crm_supplier_account.id），用于网关用量归属、密钥管理与对账。
--
-- 说明：
--   1. 列可空仅为兼容历史数据，业务保存时强制必填（后端 buildEntities 同步强校验）；
--   2. 历史数据按「同渠道下优先启用账户，其次按 id 最小」回填；渠道下没有任何账户时保持
--      null，需人工在商机/合同编辑页补选，否则保存/转合同会被后端拦截；
--   3. 全部语句幂等，可重复执行。
-- 数据库：MySQL 5.7+ / 8.0
-- ----------------------------

-- ----------------------------
-- 1、crm_business_product 增加 supplier_account_id
-- ----------------------------
set @has_col = (select count(*) from information_schema.columns
                 where table_schema = database()
                   and table_name = 'crm_business_product'
                   and column_name = 'supplier_account_id');
set @ddl = if(@has_col = 0,
  'alter table crm_business_product add column supplier_account_id bigint(20) default null comment ''渠道账户ID（crm_supplier_account.id）'' after supplier_id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、crm_contract_product 增加 supplier_account_id
-- ----------------------------
set @has_col = (select count(*) from information_schema.columns
                 where table_schema = database()
                   and table_name = 'crm_contract_product'
                   and column_name = 'supplier_account_id');
set @ddl = if(@has_col = 0,
  'alter table crm_contract_product add column supplier_account_id bigint(20) default null comment ''渠道账户ID（crm_supplier_account.id）'' after supplier_id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 3、索引（按渠道账户反查用量/明细）
-- ----------------------------
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'crm_business_product'
               and index_name = 'idx_bp_account');
set @ddl = if(@idx = 0, 'alter table crm_business_product add index idx_bp_account (supplier_account_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'crm_contract_product'
               and index_name = 'idx_cp_account');
set @ddl = if(@idx = 0, 'alter table crm_contract_product add index idx_cp_account (supplier_account_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、历史数据回填
--    取该渠道下状态为启用的账户，多个时取 id 最小者；没有启用账户则取该渠道 id 最小的账户。
--    已存在值（非 null）不回填，可重复执行。
-- ----------------------------
update crm_business_product bp
   set bp.supplier_account_id = (select a.id
                                   from crm_supplier_account a
                                  where a.supplier_id = bp.supplier_id
                                  order by (a.status = 1) desc, a.id
                                  limit 1)
 where bp.supplier_account_id is null
   and bp.supplier_id is not null;

update crm_contract_product cp
   set cp.supplier_account_id = (select a.id
                                   from crm_supplier_account a
                                  where a.supplier_id = cp.supplier_id
                                  order by (a.status = 1) desc, a.id
                                  limit 1)
 where cp.supplier_account_id is null
   and cp.supplier_id is not null;

-- ----------------------------
-- 5、回填结果自查（无法自动回填的明细，需人工补选渠道账户）
-- ----------------------------
-- select 'crm_business_product' as table_name, count(1) as missing
--   from crm_business_product where supplier_account_id is null
-- union all
-- select 'crm_contract_product' as table_name, count(1)
--   from crm_contract_product where supplier_account_id is null and deleted = 0;
