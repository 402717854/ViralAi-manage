-- ============================================================
-- 大模型网关用量自动对账（财务对账）模块
-- 依赖：crm_contract、crm_contract_product、crm_model_product、crm_supplier、crm_customer
-- 脚本幂等，可重复执行；MySQL 5.7+ / 8.0
--
-- 与需求文档的差异（已按工程实际情况兼容）：
--   1. crm_contract 的客户字段为 consumer_id（文档写作 consumer_id，一致）；
--      客户表在工程中为 crm_customer（文档示例可能写作 crm_consumer）。
--   2. crm_model_product 原无 product_code / 基准价字段，本脚本补列并回填；
--      模型编码为对账匹配键，网关 product_code 必须与之对应。
--   3. crm_contract_usage 增加 usage_type / ref_usage_id，用于差异调账的冲销流水；
--      crm_contract_usage 无 deleted 字段（台账只追加，不删除）。
--   4. crm_contract_bill_diff 增加 supplier_id / customer_id / call_no / call_time / amount，
--      差异工作台需要「类型、流水、客户、金额」，文档 DDL 缺这几列；bill_id 允许为空，
--      因为差异在 T+1 对账时即产生，此时账单尚未生成。
-- ============================================================

-- ----------------------------
-- 1、网关调用流水表
-- ----------------------------
create table if not exists gateway_call_log (
  id                bigint(20)    not null auto_increment           comment '主键',
  call_no           varchar(128)  not null                           comment '网关调用唯一流水号，幂等key',
  customer_id       bigint(20)    not null                           comment '客户ID，用于匹配合同',
  supplier_id       bigint(20)    not null                           comment '渠道ID（模型归属渠道）',
  supplier_account_id bigint(20)  not null default 0                 comment '渠道账户ID，0=未归属',
  product_code      varchar(64)   not null                           comment '模型编码 crm_model_product.product_code',
  model_type        varchar(64)   default null                       comment 'LLM/Embedding/TTS/Image',
  prompt_tokens     bigint(20)    not null default 0                 comment '输入token',
  completion_tokens bigint(20)    not null default 0                 comment '输出token',
  total_tokens      bigint(20)    not null default 0                 comment '总token',
  total_fees        decimal(20,8) not null default 0                 comment '总消耗费用',
  call_time         datetime      not null                           comment '调用发生时间（计费归属时间）',
  call_status       tinyint(4)    not null default 1                 comment '调用状态 0失败 1成功',
  err_msg           varchar(500)  default null                       comment '失败原因',
  raw_price_detail  varchar(500)  default null                       comment '网关侧原始计费信息',
  create_time       datetime      not null default current_timestamp comment '入库时间',
  deleted           tinyint(4)    not null default 0                 comment '是否删除 0否 1是',
  primary key (id),
  unique key uk_call_no (call_no),
  key idx_customer_supplier_product (customer_id, supplier_id, product_code),
  key idx_account_product_time (supplier_account_id, product_code, call_time),
  key idx_call_time (call_time),
  key idx_supplier_call_time (supplier_id, call_time)
) engine=innodb auto_increment=1 comment='模型网关调用流水';

-- ----------------------------
-- 2、对账明细台账
-- ----------------------------
create table if not exists crm_contract_usage (
  id                  bigint(20)      not null auto_increment                  comment '主键',
  call_log_id         bigint(20)      default null                              comment '网关流水ID；调账冲销流水为空',
  call_no             varchar(128)    not null                                  comment '网关流水号；调账冲销为 ADJ-{原流水号}',
  contract_id         bigint(20)      default null                              comment '匹配到的合同ID，匹配失败为null',
  contract_product_id bigint(20)      default null                              comment '合同明细ID',
  customer_id         bigint(20)      not null                                  comment '客户ID',
  supplier_id         bigint(20)      not null                                  comment '渠道ID',
  supplier_account_id bigint(20)      not null default 0                        comment '渠道账户ID，0=未归属',
  product_id          bigint(20)      default null                              comment '模型产品ID',
  product_code        varchar(64)     not null                                  comment '模型编码',
  total_tokens        bigint(20)      not null default 0                        comment '消耗token（冲销为负）',
  total_fees          decimal(20,8)   not null default 0                        comment '总消耗费用（网关上报快照，冲销为负）',
  base_sell_price     decimal(20,8)   default null                              comment '模型基准售价（每千token）',
  sell_discount       decimal(10,2)   default null                              comment '合同售卖折扣',
  sell_amount         decimal(20,8)   default null                              comment '本次向客户应收金额',
  base_cost_price     decimal(20,8)   default null                              comment '模型基准成本价（每千token）',
  cost_discount       decimal(10,2)   default null                              comment '合同成本折扣',
  cost_amount         decimal(20,8)   default null                              comment '渠道成本金额',
  gross_profit        decimal(20,8)   default null                              comment '毛利 = 应收 - 成本',
  match_status        tinyint(4)      not null default 0                        comment '0待匹配 1匹配成功 2无生效合同 3合同过期 4模型不在合同内 5渠道不匹配 6其他异常 7多份生效合同',
  match_msg           varchar(255)    default null                              comment '匹配说明',
  call_time           datetime        not null                                  comment '调用时间（计费归属）',
  bill_id             bigint(20)      default null                              comment '归属账单ID（生成账单后回填）',
  usage_type          tinyint(4)      not null default 0                        comment '用量类型 0网关计费 1调账冲销',
  ref_usage_id        bigint(20)      default null                              comment '冲销对象用量ID（usage_type=1 时有值）',
  create_time         datetime        not null default current_timestamp        comment '创建时间',
  update_time         datetime        default null on update current_timestamp  comment '更新时间',
  primary key (id),
  unique key uk_call_log_id (call_log_id),
  key idx_contract_id (contract_id),
  key idx_customer_supplier (customer_id, supplier_id),
  key idx_match_key (customer_id, supplier_id, supplier_account_id, product_code),
  key idx_call_time (call_time),
  key idx_match_status (match_status),
  key idx_bill_id (bill_id)
) engine=innodb auto_increment=1 comment='合同用量对账明细台账';

-- ----------------------------
-- 3、对账汇总账单
-- ----------------------------
create table if not exists crm_contract_bill (
  id                 bigint(20)     not null auto_increment                  comment '主键',
  bill_no            varchar(64)    not null                                 comment '账单编号',
  contract_id        bigint(20)     not null                                 comment '合同ID',
  customer_id        bigint(20)     not null                                 comment '客户ID',
  supplier_id        bigint(20)     not null                                 comment '渠道ID',
  supplier_account_id bigint(20)    not null default 0                       comment '渠道账户ID，0=未归属',
  bill_cycle         varchar(32)    not null                                 comment '账期：2026-11',
  start_time         datetime       not null                                 comment '周期起始',
  end_time           datetime       not null                                 comment '周期结束',
  total_tokens       bigint(20)     not null default 0                       comment '总token',
  total_sell_amount  decimal(20,8)  not null default 0.0000                  comment '应收客户总额',
  total_cost_amount  decimal(20,8)  not null default 0.0000                  comment '渠道总成本',
  total_gross_profit decimal(20,8)  not null default 0.0000                  comment '总毛利',
  bill_status        tinyint(4)     not null default 0                       comment '0待生成 1待复核 2已确认 3已结算 4差异挂起',
  remark             varchar(500)   default null                             comment '备注、差异说明',
  creator            bigint(20)     not null                                 comment '创建人',
  auditor            bigint(20)     default null                             comment '复核人',
  audit_time         datetime       default null                             comment '复核时间',
  create_time        datetime       not null default current_timestamp       comment '创建时间',
  update_time        datetime       default null on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_bill_no (bill_no),
  key idx_contract_id (contract_id),
  key idx_contract_account_cycle (contract_id, supplier_account_id, bill_cycle),
  key idx_supplier_bill_cycle (supplier_id, bill_cycle),
  key idx_bill_status (bill_status)
) engine=innodb auto_increment=1 comment='合同对账账单汇总表';

-- ----------------------------
-- 4、对账差异记录
-- ----------------------------
create table if not exists crm_contract_bill_diff (
  id            bigint(20)    not null auto_increment                  comment '主键',
  bill_id       bigint(20)    default null                              comment '账单ID，未生成账单时为空',
  usage_id      bigint(20)    default null                              comment '异常用量记录ID',
  diff_type     tinyint(4)    not null                                  comment '1无合同 2合同过期 3价格异常 4token不一致 5其他',
  diff_desc     varchar(500)  not null                                  comment '差异描述',
  supplier_id   bigint(20)    default null                              comment '渠道ID（差异工作台按渠道过滤）',
  customer_id   bigint(20)    default null                              comment '客户ID',
  call_no       varchar(128)  default null                              comment '网关流水号',
  call_time     datetime      default null                              comment '调用时间（归属账期）',
  amount        decimal(20,8) default null                              comment '涉及金额（应收，匹配失败时为空）',
  handle_status tinyint(4)    not null default 0                        comment '0待处理 1已调账 2豁免 3驳回',
  handle_remark varchar(500)  default null                              comment '处理意见',
  handler       bigint(20)    default null                              comment '处理人',
  handle_time   datetime      default null                              comment '处理时间',
  create_time   datetime      not null default current_timestamp        comment '创建时间',
  primary key (id),
  unique key uk_usage_id (usage_id),
  key idx_bill_id (bill_id),
  key idx_supplier_call_time (supplier_id, call_time),
  key idx_handle_status (handle_status)
) engine=innodb auto_increment=1 comment='对账差异记录';

-- ----------------------------
-- 5、crm_model_product 补列：模型编码 + 基准价（对账匹配与计费基础）
--    MySQL 5.7 不支持 add column if not exists，用存储过程判断 information_schema
-- ----------------------------
drop procedure if exists crm_add_column_if_missing;
delimiter $$
create procedure crm_add_column_if_missing(in p_table varchar(64), in p_column varchar(64), in p_ddl varchar(500))
begin
  if not exists (select 1 from information_schema.columns
                  where table_schema = database() and table_name = p_table and column_name = p_column) then
    set @crm_ddl = concat('alter table `', p_table, '` add column ', p_ddl);
    prepare crm_stmt from @crm_ddl;
    execute crm_stmt;
    deallocate prepare crm_stmt;
  end if;
end$$
delimiter ;

call crm_add_column_if_missing('crm_model_product', 'product_code',
  '`product_code` varchar(64) default null comment ''模型编码（对账匹配键，网关 product_code 需与之对应），为空时回退 no'' after `no`');
call crm_add_column_if_missing('crm_model_product', 'base_sell_price',
  '`base_sell_price` decimal(16,6) default null comment ''基准售价（每千token）'' after `price_detail`');
call crm_add_column_if_missing('crm_model_product', 'base_cost_price',
  '`base_cost_price` decimal(16,6) default null comment ''基准成本价（每千token）'' after `base_sell_price`');

-- 消耗费用：网关上报的实际计费金额；台账侧冗余一份快照，列表与详情可直接展示
call crm_add_column_if_missing('gateway_call_log', 'total_fees',
  '`total_fees` decimal(16,6) not null default 0 comment ''总消耗费用'' after `total_tokens`');
call crm_add_column_if_missing('crm_contract_usage', 'total_fees',
  '`total_fees` decimal(16,6) not null default 0 comment ''总消耗费用（网关上报快照，冲销为负）'' after `total_tokens`');

drop procedure if exists crm_add_column_if_missing;

-- ----------------------------
-- 5.1 财务对账金额列精度统一 decimal(20,8)
--      前端统一按 8 位小数展示，存储与计算精度必须同步，否则高两位恒为 0
--      modify column 幂等，可重复执行
-- ----------------------------
alter table gateway_call_log
  modify column total_fees decimal(20,8) not null default 0 comment '总消耗费用';

alter table crm_contract_usage
  modify column total_fees      decimal(20,8) not null default 0 comment '总消耗费用（网关上报快照，冲销为负）',
  modify column base_sell_price decimal(20,8) default null       comment '模型基准售价（每千token）',
  modify column sell_amount     decimal(20,8) default null       comment '本次向客户应收金额',
  modify column base_cost_price decimal(20,8) default null       comment '模型基准成本价（每千token）',
  modify column cost_amount     decimal(20,8) default null       comment '渠道成本金额',
  modify column gross_profit    decimal(20,8) default null       comment '毛利 = 应收 - 成本';

alter table crm_contract_bill
  modify column total_sell_amount  decimal(20,8) not null default 0 comment '应收客户总额',
  modify column total_cost_amount  decimal(20,8) not null default 0 comment '渠道总成本',
  modify column total_gross_profit decimal(20,8) not null default 0 comment '总毛利';

alter table crm_contract_bill_diff
  modify column amount decimal(20,8) default null comment '涉及金额（应收，匹配失败时为空）';

-- 模型编码为空时回退模型型号，保证历史数据可立即参与匹配
update crm_model_product
   set product_code = `no`
 where (product_code is null or product_code = '') and `no` is not null and `no` <> '';

-- 兼容生产库已有的重复编码：只提示，不做强制唯一，匹配时按多条识别为脏数据
-- select product_code, count(1) from crm_model_product where deleted = 0 and product_code is not null
--  group by product_code having count(1) > 1;

-- ----------------------------
-- 6、字典
-- ----------------------------
-- 6.1 字典类型
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '对账匹配状态', 'crm_usage_match_status', '0', 'admin', sysdate(), '网关用量与合同匹配结果'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_usage_match_status');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '对账账单状态', 'crm_bill_status', '0', 'admin', sysdate(), '财务对账账单流转状态'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bill_status');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '对账差异类型', 'crm_bill_diff_type', '0', 'admin', sysdate(), '对账差异分类'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bill_diff_type');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '差异处理状态', 'crm_bill_diff_handle', '0', 'admin', sysdate(), '差异处理结果'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_bill_diff_handle');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '用量类型', 'crm_usage_type', '0', 'admin', sysdate(), '网关计费 / 调账冲销'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_usage_type');

-- 6.2 匹配状态
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '待匹配', '0', 'crm_usage_match_status', '', 'info', 'N', '0', 'admin', sysdate(), '尚未执行匹配'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '匹配成功', '1', 'crm_usage_match_status', '', 'success', 'Y', '0', 'admin', sysdate(), '已匹配合同并完成计费'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '无生效合同', '2', 'crm_usage_match_status', '', 'danger', 'N', '0', 'admin', sysdate(), '客户+渠道无已签约合同'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '合同已过期', '3', 'crm_usage_match_status', '', 'warning', 'N', '0', 'admin', sysdate(), '调用时间不在合同有效期内'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '3');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '模型不在合同内', '4', 'crm_usage_match_status', '', 'danger', 'N', '0', 'admin', sysdate(), '合同明细无该模型'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '4');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 6, '渠道不匹配', '5', 'crm_usage_match_status', '', 'warning', 'N', '0', 'admin', sysdate(), '流水渠道与合同明细渠道不一致'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '5');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 7, '其他异常', '6', 'crm_usage_match_status', '', 'danger', 'N', '0', 'admin', sysdate(), '模型未维护、价格缺失等'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '6');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 8, '多份生效合同', '7', 'crm_usage_match_status', '', 'danger', 'N', '0', 'admin', sysdate(), '同一客户+渠道+账户+模型存在多份生效合同明细（脏数据，需人工处理）'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_match_status' and dict_value = '7');

-- 6.3 账单状态
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '待生成', '0', 'crm_bill_status', '', 'info', 'N', '0', 'admin', sysdate(), '预留状态'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_status' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '待复核', '1', 'crm_bill_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '生成后待财务复核'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_status' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '已确认', '2', 'crm_bill_status', '', 'primary', 'N', '0', 'admin', sysdate(), '复核通过，可结算'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_status' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '已结算', '3', 'crm_bill_status', '', 'success', 'N', '0', 'admin', sysdate(), '结算完成，回写商机结算状态'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_status' and dict_value = '3');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '差异挂起', '4', 'crm_bill_status', '', 'danger', 'N', '0', 'admin', sysdate(), '存在未处理差异，暂缓结算'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_status' and dict_value = '4');

-- 6.4 差异类型
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '无合同', '1', 'crm_bill_diff_type', '', 'danger', 'N', '0', 'admin', sysdate(), '找不到可计费合同'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_type' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '合同过期', '2', 'crm_bill_diff_type', '', 'warning', 'N', '0', 'admin', sysdate(), '调用时间超出合同有效期'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_type' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '价格异常', '3', 'crm_bill_diff_type', '', 'danger', 'N', '0', 'admin', sysdate(), '基准价或折扣缺失'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_type' and dict_value = '3');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, 'token不一致', '4', 'crm_bill_diff_type', '', 'warning', 'N', '0', 'admin', sysdate(), '网关侧token与台账不一致'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_type' and dict_value = '4');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '其他', '5', 'crm_bill_diff_type', '', 'info', 'Y', '0', 'admin', sysdate(), '模型不在合同、渠道不匹配等'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_type' and dict_value = '5');

-- 6.5 差异处理状态
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '待处理', '0', 'crm_bill_diff_handle', '', 'warning', 'Y', '0', 'admin', sysdate(), '未处理'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_handle' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '已调账', '1', 'crm_bill_diff_handle', '', 'success', 'N', '0', 'admin', sysdate(), '生成冲销用量'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_handle' and dict_value = '1');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '豁免', '2', 'crm_bill_diff_handle', '', 'info', 'N', '0', 'admin', sysdate(), '不计费，不追责'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_handle' and dict_value = '2');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '驳回', '3', 'crm_bill_diff_handle', '', 'danger', 'N', '0', 'admin', sysdate(), '退回继续核查'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_bill_diff_handle' and dict_value = '3');

-- 6.6 用量类型
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '网关计费', '0', 'crm_usage_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '网关流水匹配生成'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_type' and dict_value = '0');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '调账冲销', '1', 'crm_usage_type', '', 'warning', 'N', '0', 'admin', sysdate(), '差异处理调账生成的冲销记录'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_usage_type' and dict_value = '1');

-- ----------------------------
-- 7、菜单与按钮权限（幂等）
--    目录：CRM 根目录 → 财务对账 → 四个页面
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

-- 7.1 财务对账目录
set @finance_dir = (select menu_id from sys_menu where path = 'finance' and menu_type = 'M'
                     and parent_id = @crm_parent limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '财务对账', @crm_parent, 6, 'finance', null, '', '',
       1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', null, '网关用量对账与账单结算'
from dual where @finance_dir is null;

set @finance_dir = (select menu_id from sys_menu where path = 'finance' and menu_type = 'M'
                     and parent_id = @crm_parent limit 1);
set @finance_dir = ifnull(@finance_dir, @crm_parent);

-- 7.2 用量对账明细
set @usage_menu = (select menu_id from sys_menu where perms = 'crm:usage:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '用量对账明细', @finance_dir, 1, 'usage', 'crm/usage/index', '', '',
       1, 0, 'C', '0', '0', 'crm:usage:list', 'list', 'admin', sysdate(), '', null, '网关流水匹配台账'
from dual where @usage_menu is null;

set @usage_menu = (select menu_id from sys_menu where perms = 'crm:usage:list' limit 1);

-- 7.3 对账账单
set @bill_menu = (select menu_id from sys_menu where perms = 'crm:bill:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '对账账单', @finance_dir, 2, 'bill', 'crm/bill/index', '', '',
       1, 0, 'C', '0', '0', 'crm:bill:list', 'documentation', 'admin', sysdate(), '', null, '按渠道+账期生成的对账账单'
from dual where @bill_menu is null;

set @bill_menu = (select menu_id from sys_menu where perms = 'crm:bill:list' limit 1);

-- 7.4 差异处理工作台
set @diff_menu = (select menu_id from sys_menu where perms = 'crm:bill:diff' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '差异处理', @finance_dir, 3, 'diff', 'crm/bill/diff', '', '',
       1, 0, 'C', '0', '0', 'crm:bill:diff', 'warning', 'admin', sysdate(), '', null, '对账差异调账/豁免/驳回'
from dual where @diff_menu is null;

set @diff_menu = (select menu_id from sys_menu where perms = 'crm:bill:diff' limit 1);

-- 7.5 渠道对账看板
set @board_menu = (select menu_id from sys_menu where perms = 'crm:usage:stat' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '渠道对账看板', @finance_dir, 4, 'reconcile', 'crm/reconcile/index', '', '',
       1, 0, 'C', '0', '0', 'crm:usage:stat', 'chart', 'admin', sysdate(), '', null, '渠道维度用量与毛利看板'
from dual where @board_menu is null;

-- 7.6 用量按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '对账执行', @usage_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:usage:reconcile', '#', 'admin', sysdate(), '', null, '手动触发对账匹配'
from dual where @usage_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:usage:reconcile');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '用量导出', @usage_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:usage:export', '#', 'admin', sysdate(), '', null, '导出台账Excel'
from dual where @usage_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:usage:export');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '流水上报', @usage_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:usage:ingest', '#', 'admin', sysdate(), '', null, '网关流水批量入库（联调用）'
from dual where @usage_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:usage:ingest');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '用量全量查看', @usage_menu, 4, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:usage:all', '#', 'admin', sysdate(), '', null, '不受渠道数据权限限制'
from dual where @usage_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:usage:all');

-- 7.7 账单按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '账单生成', @bill_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:generate', '#', 'admin', sysdate(), '', null, '按渠道+账期生成账单'
from dual where @bill_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:generate');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '账单复核', @bill_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:audit', '#', 'admin', sysdate(), '', null, '复核确认或差异挂起'
from dual where @bill_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:audit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '账单结算', @bill_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:settle', '#', 'admin', sysdate(), '', null, '结算并回写商机结算状态'
from dual where @bill_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:settle');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '账单导出', @bill_menu, 4, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:export', '#', 'admin', sysdate(), '', null, '导出账单汇总Excel'
from dual where @bill_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:export');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '差异处理', @diff_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:diff-handle', '#', 'admin', sysdate(), '', null, '调账/豁免/驳回'
from dual where @diff_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:diff-handle');

-- ----------------------------
-- 8、定时任务（T+1 对账）
--    执行：每日 02:10 拉取昨日流水并对账；每月 1 日 03:10 生成上月账单
--    sys_job 已存在同 invoke_target 时不重复插入
-- ----------------------------
insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
                     status, create_by, create_time, remark)
select '网关用量对账（T+1）', 'SYSTEM', 'crmFinanceTask.reconcileYesterday()', '0 10 2 * * ?', '3', '1',
       '0', 'admin', sysdate(), '拉取昨日网关流水并匹配合同计费'
from dual where not exists (select 1 from sys_job where invoke_target = 'crmFinanceTask.reconcileYesterday()');

insert into sys_job (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent,
                     status, create_by, create_time, remark)
select '网关对账账单生成（月）', 'SYSTEM', 'crmFinanceTask.generateLastMonthBill()', '0 10 3 1 * ?', '3', '1',
       '0', 'admin', sysdate(), '每月 1 日生成上月账单'
from dual where not exists (select 1 from sys_job where invoke_target = 'crmFinanceTask.generateLastMonthBill()');
