-- ----------------------------
-- 供应商产品关联表（crm_supplier_project）
-- 供应商 : 产品(模型) = 1 : N，产品编号 project_id 对应 crm_model_product.id
-- 承载原供应商主表上的成本/售卖折扣，并按产品维度细化，同时补充代理折扣与预计日消耗
-- 数据库：MySQL 5.7+ / 8.0
-- ----------------------------

-- 说明：请勿使用 drop table。若该表已由代码生成器先行建好并已有数据，本脚本的 create 语句会被跳过，
-- 仅需执行第 2 节的唯一索引部分（执行前请先按第 3 节检查是否存在重复关联）。

-- ----------------------------
-- 1、建表（已存在则跳过）
-- ----------------------------
create table if not exists crm_supplier_project (
  id                                  bigint(20)      not null auto_increment    comment '主键',
  supplier_id                         int(11)         not null                   comment '供应商ID',
  project_id                          int(11)         not null                   comment '模型ID',
  min_cost_discount                   decimal(10,2)   default null               comment '最小成本折扣',
  max_cost_discount                   decimal(10,2)   default null               comment '最大成本折扣',
  min_sell_discount                   decimal(10,2)   default null               comment '最小售价折扣',
  max_sell_discount                   decimal(10,2)   default null               comment '最大售价折扣',
  min_agent_discount                  decimal(10,2)   default null               comment '最小代理折扣',
  max_agent_discount                  decimal(10,2)   default null               comment '最大代理折扣',
  daily_usage_amount                  decimal(10,2)   default null               comment '预计日消耗金额',
  creator                             bigint(20)      default null               comment '创建者',
  create_time                         datetime        default null               comment '创建时间',
  updater                             bigint(20)      default null               comment '更新者',
  update_time                         datetime        default null               comment '更新时间',
  primary key (id),
  key idx_supplier_project_supplier (supplier_id),
  key idx_supplier_project_project (project_id)
) engine=innodb auto_increment=1 comment = '供应商产品关联表';

-- ----------------------------
-- 2、唯一索引：同一供应商对同一模型只允许一条关联
--    与接口层「静默去重」配合，防止并发写入产生重复行
-- ----------------------------
-- 若索引已存在会报 Duplicate key name，属正常情况，可忽略后继续。
alter table crm_supplier_project
  add unique key uk_supplier_project (supplier_id, project_id);

-- ----------------------------
-- 3、加唯一索引前的重复数据检查（先执行，确认结果为空再加索引）
-- ----------------------------
-- select supplier_id, project_id, count(*) as cnt
--   from crm_supplier_project
--  group by supplier_id, project_id
-- having count(*) > 1;

-- ----------------------------
-- 4、菜单权限说明
--    供应商产品关联不单独开放菜单与接口，统一随 /crm/supplier 的新增(add)、修改(edit)、
--    详情(query) 三个权限点维护，无需新增 sys_menu 记录。
-- ----------------------------
