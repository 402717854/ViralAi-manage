-- ============================================================
-- 对账账单：按天拆分的「子账单」
-- 依赖：crm_contract_bill（父账单）、crm_contract_usage（用量台账）
-- 数据库：MySQL 5.7+ / 8.0
-- 幂等：可重复执行
--
-- 口径说明：
--   1. 父账单粒度为「合同 + 渠道 + 账期」，金额是账期汇总；
--      子账单按「父账单 + 自然日」拆分，金额按 crm_contract_usage.call_time 的自然日归集。
--   2. 生成规则：父账单与其名下子账单在同一事务内写入，父账单金额 = 子账单金额之和；
--      若某天无匹配成功的用量，则不产生该天的子账单（不生成 0 金额子账单）。
--   3. 唯一键取 (bill_id, bill_date) 而非 (contract_id, supplier_id, bill_cycle, bill_date)：
--      现有口径允许同合同同账期存在多张父账单（补充账单），
--      按合同维度建唯一键会导致补充账单无法落库；父账单内部再保证「一天一张」。
--   4. 子账单不单独维护账单状态，状态一律取父账单 bill_status（复核/结算以父账单为准），
--      避免双写产生状态不一致。
--   5. 子账单明细通过「bill_id + call_time 落在 bill_date 当天」定位，不再给
--      crm_contract_usage 增加 sub_bill_id 列：归属可由 (bill_id, 自然日) 唯一推导，
--      避免台账表追加列与历史回填。
--
-- 部署说明：本脚本不加入 deploy/mysql/init/00-ruoyi-init.sh 的 FILES 列表——
--   它依赖 crm_contract_bill 已存在，全新库初始化顺序无法保证；
--   与 crm_supplier_add_supplier_type.sql 的处理方式一致，由发布流程单独执行。
-- ============================================================

-- ----------------------------
-- 1、子账单表
-- ----------------------------
create table if not exists crm_contract_bill_sub (
  id                  bigint(20)      not null auto_increment                  comment '主键',
  sub_bill_no         varchar(64)     not null                                 comment '子账单编号：{父账单号}-D{yyyyMMdd}',
  bill_id             bigint(20)      not null                                 comment '父账单ID crm_contract_bill.id',
  bill_no             varchar(64)     not null                                 comment '父账单编号，冗余便于列表直接检索',
  bill_cycle          varchar(32)     not null                                 comment '账期 yyyy-MM，冗余便于按账期聚合',
  bill_date           date            not null                                 comment '账单日期，取用量 call_time 的自然日',
  contract_id         bigint(20)      not null                                 comment '合同ID',
  customer_id         bigint(20)      not null                                 comment '客户ID',
  supplier_id         bigint(20)      not null                                 comment '渠道ID',
  supplier_account_id bigint(20)      not null default 0                       comment '渠道账户ID，0=未归属（与父账单口径一致，当前固定写0）',
  usage_count         int(11)         not null default 0                       comment '归集用量条数',
  total_tokens        bigint(20)      not null default 0                       comment '当日总token',
  total_sell_amount   decimal(20,8)   not null default 0.00000000              comment '当日应收金额',
  total_cost_amount   decimal(20,8)   not null default 0.00000000              comment '当日渠道成本',
  total_gross_profit  decimal(20,8)   not null default 0.00000000              comment '当日毛利 = 应收 - 成本',
  creator             bigint(20)      not null                                 comment '创建人（与父账单一致，系统生成时为系统用户）',
  create_time         datetime        not null default current_timestamp       comment '创建时间',
  update_time         datetime        default null on update current_timestamp comment '更新时间',
  primary key (id),
  unique key uk_sub_bill_no (sub_bill_no),
  unique key uk_bill_date (bill_id, bill_date),
  key idx_contract_cycle_date (contract_id, bill_cycle, bill_date),
  key idx_supplier_cycle_date (supplier_id, bill_cycle, bill_date)
) engine=innodb auto_increment=1 comment='合同对账子账单（父账单按天拆分）';

-- ----------------------------
-- 2、用量表补充复合索引（建议执行）
--    子账单明细查询条件为 bill_id = ? and call_time >= 当日00:00:00 and call_time < 次日00:00:00，
--    现有索引只有单列 idx_bill_id 与 idx_call_time，补 (bill_id, call_time) 避免大账单回表过滤。
-- ----------------------------
set @has_index = (
  select count(*) from information_schema.statistics
   where table_schema = database()
     and table_name = 'crm_contract_usage'
     and index_name = 'idx_bill_call_time'
);
set @ddl = if(@has_index = 0,
  'alter table crm_contract_usage add key idx_bill_call_time (bill_id, call_time)',
  'select ''index idx_bill_call_time already exists'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 3、历史账单回填（可选）
--    仅当需要让存量账单也能按天查看时执行；新账期生成账单时会自动写子账单。
--    以 (bill_id, bill_date) 判重，可重复执行；重复执行会按用量台账重算当日金额。
-- ----------------------------
insert into crm_contract_bill_sub
  (sub_bill_no, bill_id, bill_no, bill_cycle, bill_date,
   contract_id, customer_id, supplier_id, supplier_account_id,
   usage_count, total_tokens, total_sell_amount, total_cost_amount, total_gross_profit,
   creator, create_time)
select concat(b.bill_no, '-D', date_format(date(u.call_time), '%Y%m%d')) as sub_bill_no,
       b.id                                                               as bill_id,
       b.bill_no                                                          as bill_no,
       b.bill_cycle                                                       as bill_cycle,
       date(u.call_time)                                                  as bill_date,
       b.contract_id, b.customer_id, b.supplier_id, b.supplier_account_id,
       count(1)                                                           as usage_count,
       ifnull(sum(u.total_tokens), 0)                                     as total_tokens,
       ifnull(sum(u.sell_amount), 0)                                      as total_sell_amount,
       ifnull(sum(u.cost_amount), 0)                                      as total_cost_amount,
       ifnull(sum(u.gross_profit), 0)                                     as total_gross_profit,
       b.creator,
       now()
  from crm_contract_usage u
  join crm_contract_bill b on b.id = u.bill_id
 where u.bill_id is not null
 group by b.id, b.bill_no, b.bill_cycle, date(u.call_time),
          b.contract_id, b.customer_id, b.supplier_id, b.supplier_account_id, b.creator
on duplicate key update
  usage_count        = values(usage_count),
  total_tokens       = values(total_tokens),
  total_sell_amount  = values(total_sell_amount),
  total_cost_amount  = values(total_cost_amount),
  total_gross_profit = values(total_gross_profit),
  update_time        = now();

-- ----------------------------
-- 4、一致性校验（人工执行，应返回 0 行）
--    有子账单的父账单，其金额必须等于子账单合计；返回记录表示需要重跑第 3 段回填。
-- ----------------------------
-- select b.id, b.bill_no,
--        b.total_sell_amount, s.sub_sell_amount,
--        b.total_cost_amount, s.sub_cost_amount,
--        b.total_gross_profit, s.sub_gross_profit
--   from crm_contract_bill b
--   join (select bill_id,
--                sum(total_sell_amount)  as sub_sell_amount,
--                sum(total_cost_amount)  as sub_cost_amount,
--                sum(total_gross_profit) as sub_gross_profit
--           from crm_contract_bill_sub
--          group by bill_id) s on s.bill_id = b.id
--  where b.total_sell_amount  <> s.sub_sell_amount
--     or b.total_cost_amount  <> s.sub_cost_amount
--     or b.total_gross_profit <> s.sub_gross_profit;

-- ----------------------------
-- 5、隐藏菜单：子账单明细（独立二级页面）
--    RuoYi 只为目录型菜单（menu_type = 'M'）递归生成子路由，隐藏页必须挂在财务对账目录下，
--    与「商机详情」crm/business/view 的处理方式一致。
--    访问路径：{财务对账目录 path}/bill/sub，即 /finance/bill/sub?billId=xxx
--    依赖：crm_finance_reconcile.sql 已创建「财务对账」目录；目录不存在时本段不写入。
-- ----------------------------
set @finance_dir = (
  select menu_id from sys_menu
   where menu_type = 'M' and path = 'finance'
   order by menu_id limit 1
);

set @sub_menu = (select menu_id from sys_menu where perms = 'crm:bill:sub-page' limit 1);
set @next_id = (select ifnull(max(menu_id), 0) + 1 from sys_menu);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select @next_id, '子账单明细', @finance_dir, 5, 'bill/sub', 'crm/bill/sub', '', '',
       1, 0, 'C', '1', '0', 'crm:bill:sub-page', '#', 'admin', sysdate(), '', null,
       '按自然日拆分的子账单明细，隐藏路由，从对账账单列表进入'
from dual where @sub_menu is null and @finance_dir is not null;

set @sub_menu = (select menu_id from sys_menu where perms = 'crm:bill:sub-page' limit 1);

-- 5.1 授权：把子账单页同步给已拥有「对账账单」查看权限的角色
--     隐藏页同样受角色菜单控制，未授权时从列表跳转会 404；admin 为超管，无需授权。
insert into sys_role_menu (role_id, menu_id)
select distinct rm.role_id, @sub_menu
  from sys_role_menu rm
  join sys_menu m on m.menu_id = rm.menu_id
 where m.perms = 'crm:bill:list'
   and @sub_menu is not null
   and not exists (
     select 1 from sys_role_menu x where x.role_id = rm.role_id and x.menu_id = @sub_menu
   );

-- ----------------------------
-- 6、回滚（谨慎，确认无业务依赖后执行）
-- ----------------------------
-- drop table if exists crm_contract_bill_sub;
-- alter table crm_contract_usage drop key idx_bill_call_time;
-- delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms = 'crm:bill:sub-page');
-- delete from sys_menu where perms = 'crm:bill:sub-page';
