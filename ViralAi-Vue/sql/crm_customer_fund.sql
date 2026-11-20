-- ----------------------------
-- 客户资金管理（P0 + P1）
-- 表：crm_customer_account / crm_customer_account_flow / crm_customer_recharge / crm_customer_refund
-- 扩展：crm_contract_bill 增加结算方式、实结金额、资金流水、结算时间
-- 数据库：MySQL 5.7+ / 8.0
-- 说明：本脚本可重复执行（幂等）；建表 if not exists，加列用存储过程判断信息架构，
--       菜单与授权用 not exists 判断。
-- ----------------------------

-- ----------------------------
-- 1、客户资金账户（1 客户 1 行）
--    不把余额挂在 crm_customer 上：客户列表/下拉是高频查询，且资金变动需锁账户行，
--    不应连带锁客户主表导致客户编辑与结算互相阻塞。
-- ----------------------------
create table if not exists crm_customer_account (
  id             bigint(20)     not null auto_increment                  comment '主键',
  customer_id    bigint(20)     not null                                 comment '客户ID crm_customer.id',
  balance        decimal(20,8)  not null default 0.00000000              comment '账面余额',
  frozen_amount  decimal(20,8)  not null default 0.00000000              comment '冻结金额（当前未启用，保留字段）',
  total_recharge decimal(20,8)  not null default 0.00000000              comment '累计充值',
  total_deduct   decimal(20,8)  not null default 0.00000000              comment '累计结算扣减',
  total_refund   decimal(20,8)  not null default 0.00000000              comment '累计退款',
  version        int(11)        not null default 0                       comment '乐观锁版本号',
  status         tinyint(4)     not null default 1                       comment '1正常 0冻结',
  last_flow_time datetime       default null                             comment '最近一次资金变动时间',
  create_time    datetime       not null default current_timestamp       comment '创建时间',
  update_time    datetime       default null on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_customer_id (customer_id)
) engine=innodb auto_increment=1 comment='客户资金账户';

-- ----------------------------
-- 2、客户资金流水（只增不改，错误用反向流水冲正）
--    uk_biz 是结算幂等的基石：一张账单只可能有一条结算扣款流水。
-- ----------------------------
create table if not exists crm_customer_account_flow (
  id             bigint(20)     not null auto_increment                  comment '主键',
  flow_no        varchar(64)    not null                                 comment '流水号 FL-{yyyyMMdd}-{6位序列}',
  account_id     bigint(20)     not null                                 comment '账户ID',
  customer_id    bigint(20)     not null                                 comment '客户ID（冗余，便于按客户检索）',
  biz_type       tinyint(4)     not null                                 comment '1充值 2结算扣款 3退款 4人工调增 5人工调减 6冲销退回 7期初余额 8结算撤销',
  biz_id         bigint(20)     not null                                 comment '业务单据ID（充值单/账单/退款单/原流水）',
  biz_no         varchar(64)    default null                             comment '业务单据编号（冗余便于展示）',
  direction      tinyint(4)     not null                                 comment '1入账 2出账',
  amount         decimal(20,8)  not null                                 comment '变动金额，恒为正数，方向由 direction 表示',
  balance_before decimal(20,8)  not null                                 comment '变动前余额',
  balance_after  decimal(20,8)  not null                                 comment '变动后余额',
  bill_id        bigint(20)     default null                             comment '关联账单ID',
  bill_no        varchar(64)    default null                             comment '关联账单编号',
  contract_id    bigint(20)     default null                             comment '关联合同ID',
  remark         varchar(500)   default null                             comment '备注',
  operator       bigint(20)     not null                                 comment '操作人',
  create_time    datetime       not null default current_timestamp       comment '创建时间',
  primary key (id),
  unique key uk_flow_no (flow_no),
  unique key uk_biz (biz_type, biz_id),
  key idx_customer_time (customer_id, create_time),
  key idx_bill_id (bill_id),
  key idx_account_id (account_id)
) engine=innodb auto_increment=1 comment='客户资金流水';

-- ----------------------------
-- 3、充值单
--    uk_customer_serial：同一客户同一银行流水号只能登记一次（NULL 不参与唯一约束）
-- ----------------------------
create table if not exists crm_customer_recharge (
  id             bigint(20)     not null auto_increment                  comment '主键',
  recharge_no    varchar(64)    not null                                 comment '充值单号 RC-{yyyyMMdd}-{6位序列}',
  customer_id    bigint(20)     not null                                 comment '客户ID',
  amount         decimal(20,8)  not null                                 comment '充值金额',
  pay_type       tinyint(4)     not null default 1                       comment '1对公转账 2在线支付 3现金 4其他',
  payer_name     varchar(200)   default null                             comment '付款方名称',
  payer_account  varchar(64)    default null                             comment '付款账号（建议只存后四位）',
  pay_time       datetime       default null                             comment '到账时间',
  bank_serial_no varchar(128)   default null                             comment '银行流水号',
  voucher_url    varchar(500)   default null                             comment '凭证附件地址',
  status         tinyint(4)     not null default 0                       comment '0待审核 1已入账 2已驳回',
  auditor        bigint(20)     default null                             comment '审核人',
  audit_time     datetime       default null                             comment '审核时间',
  audit_remark   varchar(500)   default null                             comment '审核意见',
  remark         varchar(500)   default null                             comment '备注',
  creator        bigint(20)     not null                                 comment '登记人',
  create_time    datetime       not null default current_timestamp       comment '创建时间',
  update_time    datetime       default null on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_recharge_no (recharge_no),
  unique key uk_customer_serial (customer_id, bank_serial_no),
  key idx_customer_status (customer_id, status),
  key idx_status_create (status, create_time)
) engine=innodb auto_increment=1 comment='客户充值单';

-- ----------------------------
-- 4、退款单
-- ----------------------------
create table if not exists crm_customer_refund (
  id              bigint(20)     not null auto_increment                 comment '主键',
  refund_no       varchar(64)    not null                                comment '退款单号 RF-{yyyyMMdd}-{6位序列}',
  customer_id     bigint(20)     not null                                comment '客户ID',
  amount          decimal(20,8)  not null                                comment '退款金额',
  refund_type     tinyint(4)     not null default 1                      comment '1原路退回 2其他方式',
  receive_name    varchar(200)   default null                            comment '收款户名',
  receive_account varchar(64)    default null                            comment '收款账号',
  receive_bank    varchar(200)   default null                            comment '开户行',
  reason          varchar(500)   not null                                comment '退款原因',
  status          tinyint(4)     not null default 0                      comment '0待审批 1已退款 2已驳回',
  auditor         bigint(20)     default null                            comment '审批人',
  audit_time      datetime       default null                            comment '审批时间',
  audit_remark    varchar(500)   default null                            comment '审批意见',
  creator         bigint(20)     not null                                comment '申请人',
  create_time     datetime       not null default current_timestamp      comment '创建时间',
  update_time     datetime       default null on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_refund_no (refund_no),
  key idx_customer_status (customer_id, status),
  key idx_status_create (status, create_time)
) engine=innodb auto_increment=1 comment='客户退款单';

-- ----------------------------
-- 5、人工调账单
--    调账没有业务单据，但资金流水需要可追溯的 biz_id（唯一键的一部分），
--    因此单独建表生成调账单号，而不是拿易变的数值硬凑 biz_id。
-- ----------------------------
create table if not exists crm_customer_fund_adjust (
  id          bigint(20)     not null auto_increment                  comment '主键',
  adjust_no   varchar(64)    not null                                 comment '调账单号 AD-{yyyyMMdd}-{6位序列}',
  customer_id bigint(20)     not null                                 comment '客户ID',
  amount      decimal(20,8)  not null                                 comment '调账金额（正数）',
  direction   tinyint(4)     not null                                 comment '1调增 2调减',
  reason      varchar(500)   not null                                 comment '调账原因',
  creator     bigint(20)     not null                                 comment '操作人',
  create_time datetime       not null default current_timestamp       comment '创建时间',
  primary key (id),
  unique key uk_adjust_no (adjust_no),
  key idx_customer_time (customer_id, create_time)
) engine=innodb auto_increment=1 comment='客户资金人工调账单';

-- ----------------------------
-- 6、账单表扩展：结算方式、实结金额、资金流水、结算时间
--    MySQL 5.7 不支持 add column if not exists，沿用项目已有的存储过程判断写法
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

call crm_add_column_if_missing('crm_contract_bill', 'settle_type',
  '`settle_type` tinyint(4) not null default 0 comment ''结算方式 0未结算 1资金扣减 2线下收款'' after `bill_status`');
call crm_add_column_if_missing('crm_contract_bill', 'settled_amount',
  '`settled_amount` decimal(20,8) default null comment ''实结金额'' after `total_gross_profit`');
call crm_add_column_if_missing('crm_contract_bill', 'fund_flow_id',
  '`fund_flow_id` bigint(20) default null comment ''关联资金流水ID'' after `settled_amount`');
call crm_add_column_if_missing('crm_contract_bill', 'settle_time',
  '`settle_time` datetime default null comment ''结算时间'' after `fund_flow_id`');

drop procedure if exists crm_add_column_if_missing;

-- ----------------------------
-- 7、菜单与按钮权限（幂等）
--    目录：CRM → 财务对账 → 客户资金（order 5，排在账单/差异/看板之后）
-- ----------------------------
set @crm_parent = (select menu_id from sys_menu
                    where menu_type = 'M' and parent_id = 0
                      and (path = 'crm' or menu_name like '%CRM%')
                    order by (path = 'crm') desc, menu_id
                    limit 1);
set @crm_parent = ifnull(@crm_parent, 0);

set @finance_dir = (select menu_id from sys_menu where path = 'finance' and menu_type = 'M'
                     and parent_id = @crm_parent limit 1);
set @finance_dir = ifnull(@finance_dir, @crm_parent);

set @fund_menu = (select menu_id from sys_menu where perms = 'crm:fund:list' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '客户资金', @finance_dir, 5, 'fund', 'crm/fund/index', '', '',
       1, 0, 'C', '0', '0', 'crm:fund:list', 'wallet', 'admin', sysdate(), '', null, '客户充值余额、流水与结算扣减'
from dual where @fund_menu is null;

set @fund_menu = (select menu_id from sys_menu where perms = 'crm:fund:list' limit 1);

-- 7.1 按钮权限
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '充值登记', @fund_menu, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:recharge', '#', 'admin', sysdate(), '', null, '登记客户打款充值单'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:recharge');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '充值审核', @fund_menu, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:recharge-audit', '#', 'admin', sysdate(), '', null, '审核充值单并入账'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:recharge-audit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '退款申请', @fund_menu, 3, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:refund', '#', 'admin', sysdate(), '', null, '发起客户余额退款'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:refund');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '退款审批', @fund_menu, 4, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:refund-audit', '#', 'admin', sysdate(), '', null, '审批退款并扣减余额'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:refund-audit');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '人工调账', @fund_menu, 5, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:adjust', '#', 'admin', sysdate(), '', null, '人工调增/调减客户余额'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:adjust');

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '资金导出', @fund_menu, 6, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:fund:export', '#', 'admin', sysdate(), '', null, '导出账户与流水'
from dual where @fund_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:fund:export');

-- 7.2 账单结算撤销按钮（挂在已有「对账账单」菜单下）
set @bill_menu = (select menu_id from sys_menu where perms = 'crm:bill:list' limit 1);
insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '结算撤销', @bill_menu, 5, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:bill:settle-cancel', '#', 'admin', sysdate(), '', null, '撤销结算并红冲资金流水'
from dual where @bill_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:bill:settle-cancel');

-- 7.3 角色授权：已将「对账账单」授予的角色，同步授予「客户资金」
--     明细授权由管理员按角色职责勾选（销售只读），此处保证财务角色不会因漏配而看不到入口
insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, m.menu_id
from sys_role_menu rm
join sys_menu src on rm.menu_id = src.menu_id and src.perms = 'crm:bill:list'
join sys_menu m on m.perms in ('crm:fund:list', 'crm:fund:recharge', 'crm:fund:recharge-audit',
                               'crm:fund:refund', 'crm:fund:refund-audit', 'crm:fund:adjust',
                               'crm:fund:export', 'crm:bill:settle-cancel')
where not exists (select 1 from sys_role_menu x where x.role_id = rm.role_id and x.menu_id = m.menu_id);

-- ----------------------------
-- 8、上线切换：期初余额
--    历史已结算账单不回溯补流水（历史结算本无资金动作，补出来是虚假数据）。
--    期初余额通过页面「人工调账（期初余额）」或下列导入语句写入，写入后余额即为真实在账余额。
--    示例：生成期初流水（先确认 crm_customer_account 已存在对应客户账户）
-- ----------------------------
-- insert into crm_customer_account (customer_id, balance, total_recharge, total_deduct, total_refund, version, status, last_flow_time)
-- select c.id, 0, 0, 0, 0, 0, 1, sysdate() from crm_customer c where c.deleted = 0
--   and not exists (select 1 from crm_customer_account a where a.customer_id = c.id);
-- -- 期初金额由财务逐户录入（建议走页面批量导入接口，避免人工拼 SQL 出错）

-- ----------------------------
-- 9、回滚（谨慎，确认无资金数据后再执行）
-- ----------------------------
-- alter table crm_contract_bill drop column settle_type, drop column settled_amount,
--   drop column fund_flow_id, drop column settle_time;
-- delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms like 'crm:fund:%' or perms = 'crm:bill:settle-cancel');
-- delete from sys_menu where perms like 'crm:fund:%' or perms = 'crm:bill:settle-cancel';
-- drop table if exists crm_customer_fund_adjust;
-- drop table if exists crm_customer_refund;
-- drop table if exists crm_customer_recharge;
-- drop table if exists crm_customer_account_flow;
-- drop table if exists crm_customer_account;
