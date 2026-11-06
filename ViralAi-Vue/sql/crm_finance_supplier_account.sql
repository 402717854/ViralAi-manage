-- ----------------------------
-- 对账链路增加「渠道账户」维度（supplier_account_id）
--
-- 背景：
--   渠道账户（crm_supplier_account）已在商机明细/合同明细（crm_business_product、
--   crm_contract_product）落库，用于网关用量归属与密钥管理。本脚本把账户维度
--   贯通到对账链路：网关流水 → 用量台账 → 对账账单。
--
-- 语义约定：
--   0 表示「账户未归属」（历史数据、网关未上报账户时的兼容值），不使用 null，
--   避免唯一键/分组聚合下 null 不参与比较导致重复账单。
--
-- 说明：
--   1. 全部语句幂等，可重复执行；数据库 MySQL 5.7+ / 8.0；
--   2. 账单唯一性口径已确认为「允许补充账单」：不建 uk_bill，只建普通索引
--      idx_contract_account_cycle，账单生成逻辑不变（详见第 7 节）；
--   3. 历史数据回填只做「可确定」的部分，无法确定的保持 0，由第 5 节自查暴露。
-- ----------------------------

-- ----------------------------
-- 1、gateway_call_log 增加 supplier_account_id
-- ----------------------------
set @has_col = (select count(*) from information_schema.columns
                 where table_schema = database()
                   and table_name = 'gateway_call_log'
                   and column_name = 'supplier_account_id');
set @ddl = if(@has_col = 0,
  'alter table gateway_call_log add column supplier_account_id bigint(20) not null default 0 comment ''渠道账户ID（crm_supplier_account.id），0=未归属'' after supplier_id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 账户 + 模型 + 时间：按账户维度拉取/统计流水
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'gateway_call_log'
               and index_name = 'idx_account_product_time');
set @ddl = if(@idx = 0,
  'alter table gateway_call_log add index idx_account_product_time (supplier_account_id, product_code, call_time)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、crm_contract_usage 增加 supplier_account_id
-- ----------------------------
set @has_col = (select count(*) from information_schema.columns
                 where table_schema = database()
                   and table_name = 'crm_contract_usage'
                   and column_name = 'supplier_account_id');
set @ddl = if(@has_col = 0,
  'alter table crm_contract_usage add column supplier_account_id bigint(20) not null default 0 comment ''渠道账户ID（crm_supplier_account.id），0=未归属'' after supplier_id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 匹配键：客户 + 渠道 + 账户 + 模型
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'crm_contract_usage'
               and index_name = 'idx_match_key');
set @ddl = if(@idx = 0,
  'alter table crm_contract_usage add index idx_match_key (customer_id, supplier_id, supplier_account_id, product_code)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 3、crm_contract_bill 增加 supplier_account_id
-- ----------------------------
set @has_col = (select count(*) from information_schema.columns
                 where table_schema = database()
                   and table_name = 'crm_contract_bill'
                   and column_name = 'supplier_account_id');
set @ddl = if(@has_col = 0,
  'alter table crm_contract_bill add column supplier_account_id bigint(20) not null default 0 comment ''渠道账户ID（crm_supplier_account.id），0=未归属'' after supplier_id',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 账单查询/防重索引（普通索引，唯一键待业务确认后替换，见第 6 节）
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database()
               and table_name = 'crm_contract_bill'
               and index_name = 'idx_contract_account_cycle');
set @ddl = if(@idx = 0,
  'alter table crm_contract_bill add index idx_contract_account_cycle (contract_id, supplier_account_id, bill_cycle)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、历史数据回填（只回填可确定项，不做猜测）
-- ----------------------------
-- 4.1 台账：取匹配到的合同明细上的账户
update crm_contract_usage u
  join crm_contract_product cp on cp.id = u.contract_product_id
   set u.supplier_account_id = cp.supplier_account_id
 where u.supplier_account_id = 0
   and cp.supplier_account_id is not null
   and cp.supplier_account_id <> 0;

-- 4.2 流水：按流水对应的台账账户回填（台账已归户的才算确定）
update gateway_call_log g
  join crm_contract_usage u on u.call_log_id = g.id
   set g.supplier_account_id = u.supplier_account_id
 where g.supplier_account_id = 0
   and u.supplier_account_id <> 0;

-- 4.3 账单：仅当账单下全部用量归属同一账户时才回填；跨账户的历史账单保持 0，
--     避免把多个账户的金额错误挂到某一个账户上
update crm_contract_bill b
  join (select bill_id,
               min(supplier_account_id) as account_id,
               count(distinct supplier_account_id) as account_cnt
          from crm_contract_usage
         where bill_id is not null
         group by bill_id) t on t.bill_id = b.id
   set b.supplier_account_id = t.account_id
 where b.supplier_account_id = 0
   and t.account_cnt = 1
   and t.account_id <> 0;

-- ----------------------------
-- 5、存量合规性自查（执行唯一键前必须先看）
-- ----------------------------
-- 5.1 同 合同 + 账户 + 账期 的账单分布：口径 B 下允许重复（补充账单），此查询仅作数据可见性检查
select contract_id, supplier_account_id, bill_cycle, count(1) as dup_cnt
  from crm_contract_bill
 group by contract_id, supplier_account_id, bill_cycle
having count(1) > 1;

-- 5.2 未归属账户的数据量
select 'gateway_call_log' as table_name, count(1) as unattributed
  from gateway_call_log where supplier_account_id = 0
union all
select 'crm_contract_usage', count(1) from crm_contract_usage where supplier_account_id = 0
union all
select 'crm_contract_bill', count(1) from crm_contract_bill where supplier_account_id = 0;

-- ----------------------------
-- 6、匹配状态字典新增「多份生效合同」（match_status = 7）
--    口径：同一客户 + 渠道 + 账户 + 模型，同一时间只允许一份生效合同明细；
--    多于一份即脏数据，对账标记异常并告警，由差异工作台人工处理。
-- ----------------------------
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 8, '多份生效合同', '7', 'crm_usage_match_status', '', 'danger', 'N', '0', 'admin', sysdate(), '同一客户+渠道+账户+模型存在多份生效合同明细（脏数据，需人工处理）'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '7');

-- ----------------------------
-- 7、账单唯一性口径（已确认：允许补充账单）
--
-- 结论：**不建** uk_bill (contract_id, supplier_account_id, bill_cycle)，仅保留
--       普通索引 idx_contract_account_cycle（第 3 节已建），账单生成逻辑保持不变。
--
-- 依据：
--   1. 同一合同同账期允许多张账单（补充账单），用于网关补推、补签合同、任务重跑等场景，
--      CrmContractBillServiceImpl#generateForSupplier 在 countByContractAndCycle > 0 时
--      会再插一张并在 remark 标注「补充账单（同账期已有 N 张）」；
--   2. 若建唯一键，该分支第二次 insert 必抛 DuplicateKeyException，属于破坏现有行为；
--   3. 账单唯一性由 bill_no 上的 uk_bill_no 保证，一账期多账单不影响幂等（bill_no 按
--      渠道 + 账期 + 序列生成，不会重复）；
--   4. 跨账户汇总是当前口径：账单的 supplier_account_id 保持 0（未归属），
--      按账户出账需同时改造 selectBillGroups / updateBillIdByContract / 账单号规则，
--      不在本次变更范围内。
--
-- 若将来改为一账期一账单（口径 A），需同时完成：
--   a) 删除生成逻辑中的「补充账单」分支；
--   b) 清理存量重复数据（见 5.1 自查）；
--   c) 再执行 add unique key uk_bill (contract_id, supplier_account_id, bill_cycle)。
-- ----------------------------
