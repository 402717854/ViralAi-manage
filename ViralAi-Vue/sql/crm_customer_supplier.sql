-- ----------------------------
-- 客户-渠道关系重构
--   旧模型：商机/合同各自绑定一个渠道（一对一）
--   新模型：商机/合同只绑定客户；客户关联多个渠道；渠道关联多个模型；
--           具体渠道由商机/合同的明细行（crm_business_product / crm_contract_product）承载
--
-- 本脚本内容：
--   1. 新建 crm_customer_supplier（客户-渠道关联表）
--   2. 回填历史数据：从既有商机、合同及其明细反推客户-渠道组合
--   3. 将 crm_business.supplier_id / crm_contract.supplier_id 改为可空（列保留但不再写入，便于回滚）
--   4. 补关联表索引
--   5. 补客户菜单「关联渠道」按钮权限
--   6. 重建明细表唯一索引为「主表 + 渠道 + 模型」（允许同一模型挂多个渠道）
--
-- 全部语句幂等，可重复执行。
-- ----------------------------

-- ----------------------------
-- 1、客户-渠道关联表
-- ----------------------------
create table if not exists `crm_customer_supplier` (
  `id`          bigint      not null auto_increment comment '主键',
  `customer_id` bigint      not null comment '客户ID',
  `supplier_id` bigint      not null comment '渠道ID',
  `creator`     bigint      default null comment '创建者',
  `create_time` datetime    default null comment '创建时间',
  primary key (`id`),
  unique key `uk_customer_supplier` (`customer_id`, `supplier_id`),
  key `idx_cs_supplier` (`supplier_id`)
) engine=innodb auto_increment=1 comment = '客户渠道关联表';

-- ----------------------------
-- 2、历史数据回填（insert ignore 依赖 uk_customer_supplier 去重）
-- ----------------------------
-- 2.1 商机主表上的旧渠道关系
insert ignore into crm_customer_supplier (customer_id, supplier_id, creator, create_time)
select b.customer_id, b.supplier_id, 1, now()
from crm_business b
where b.deleted = 0 and b.customer_id is not null and b.supplier_id is not null;

-- 2.2 商机明细上的渠道（可能比主表更准确，已存在组合会被 ignore）
insert ignore into crm_customer_supplier (customer_id, supplier_id, creator, create_time)
select b.customer_id, bp.supplier_id, 1, now()
from crm_business_product bp
inner join crm_business b on bp.bus_id = b.id
where b.deleted = 0 and b.customer_id is not null and bp.supplier_id is not null;

-- 2.3 合同主表上的旧渠道关系
insert ignore into crm_customer_supplier (customer_id, supplier_id, creator, create_time)
select c.consumer_id, c.supplier_id, 1, now()
from crm_contract c
where c.deleted = 0 and c.consumer_id is not null and c.supplier_id is not null;

-- 2.4 合同明细上的渠道
insert ignore into crm_customer_supplier (customer_id, supplier_id, creator, create_time)
select c.consumer_id, cp.supplier_id, 1, now()
from crm_contract_product cp
inner join crm_contract c on cp.contract_id = c.id
where c.deleted = 0 and c.consumer_id is not null and cp.supplier_id is not null;

-- ----------------------------
-- 3、放开主表旧渠道列的非空约束
--    代码不再写入该列；若列仍为 NOT NULL，去掉渠道后插入会直接失败
-- ----------------------------
set @nullable = (select is_nullable from information_schema.columns
                  where table_schema = database() and table_name = 'crm_business' and column_name = 'supplier_id');
set @col_type = (select column_type from information_schema.columns
                  where table_schema = database() and table_name = 'crm_business' and column_name = 'supplier_id');
set @ddl = if(@nullable = 'NO',
  concat('alter table crm_business modify column supplier_id ', @col_type, ' null comment ''已废弃：渠道改由明细行承载'''),
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @nullable = (select is_nullable from information_schema.columns
                  where table_schema = database() and table_name = 'crm_contract' and column_name = 'supplier_id');
set @col_type = (select column_type from information_schema.columns
                  where table_schema = database() and table_name = 'crm_contract' and column_name = 'supplier_id');
set @ddl = if(@nullable = 'NO',
  concat('alter table crm_contract modify column supplier_id ', @col_type, ' null comment ''已废弃：渠道改由明细行承载'''),
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、辅助索引：按客户查渠道、按客户+渠道校验明细归属
-- ----------------------------
set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business_product' and index_name = 'idx_bp_supplier');
set @ddl = if(@idx = 0, 'alter table crm_business_product add index idx_bp_supplier (supplier_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_contract_product' and index_name = 'idx_cp_supplier');
set @ddl = if(@idx = 0, 'alter table crm_contract_product add index idx_cp_supplier (supplier_id)', 'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 5、客户菜单补「关联渠道」按钮权限（编辑客户时维护渠道）
-- ----------------------------
set @customer_menu = (select menu_id from sys_menu where perms = 'crm:customer:list' limit 1);

insert into sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
                      is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time,
                      update_by, update_time, remark)
select (select ifnull(max(menu_id), 0) + 1 from sys_menu), '客户渠道维护', @customer_menu, 10, '', '', '', '',
       1, 0, 'F', '0', '0', 'crm:customer:supplier', '#', 'admin', sysdate(), '', null, '维护客户可使用的渠道'
from dual where @customer_menu is not null
  and not exists (select 1 from sys_menu where perms = 'crm:customer:supplier');

-- ----------------------------
-- 6、明细表唯一索引重建：「主表 + 模型」改为「主表 + 渠道 + 模型」
--    旧唯一键会让「同一模型挂在不同渠道下」被数据库直接拒绝（Duplicate entry），
--    业务层已改为按「渠道 + 模型」去重，这里同步放宽索引。
--    先删除该表上除主键外的全部唯一索引（名称不一定统一，动态拼接），再建新的复合唯一键。
-- ----------------------------
-- 6.1 去重兜底：历史数据若存在同一「主表 + 渠道 + 模型」重复行，只保留 id 最小的一条，
--     否则新唯一索引会因重复数据建立失败
delete t1 from crm_business_product t1
inner join crm_business_product t2
        on t1.bus_id = t2.bus_id
       and t1.supplier_id = t2.supplier_id
       and t1.product_id = t2.product_id
       and t1.id > t2.id;

delete t1 from crm_contract_product t1
inner join crm_contract_product t2
        on t1.contract_id = t2.contract_id
       and t1.supplier_id = t2.supplier_id
       and t1.product_id = t2.product_id
       and t1.id > t2.id;

set @uk = (select group_concat(distinct index_name)
             from information_schema.statistics
            where table_schema = database() and table_name = 'crm_business_product'
              and non_unique = 0 and index_name <> 'primary');
set @ddl = if(@uk is null, 'select 1',
  concat('alter table crm_business_product drop index `',
         replace(@uk, ',', '`, drop index `'), '`'));
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_business_product'
               and index_name = 'uk_bp_supplier_product');
set @ddl = if(@idx = 0,
  'alter table crm_business_product add unique key uk_bp_supplier_product (bus_id, supplier_id, product_id)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @uk = (select group_concat(distinct index_name)
             from information_schema.statistics
            where table_schema = database() and table_name = 'crm_contract_product'
              and non_unique = 0 and index_name <> 'primary');
set @ddl = if(@uk is null, 'select 1',
  concat('alter table crm_contract_product drop index `',
         replace(@uk, ',', '`, drop index `'), '`'));
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @idx = (select count(*) from information_schema.statistics
             where table_schema = database() and table_name = 'crm_contract_product'
               and index_name = 'uk_cp_supplier_product');
set @ddl = if(@idx = 0,
  'alter table crm_contract_product add unique key uk_cp_supplier_product (contract_id, supplier_id, product_id)',
  'select 1');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;
