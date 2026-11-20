-- ----------------------------
-- 供应商产品关联：新增「模型所属供应商ID」
--   背景：同一供应商的报价单里，可以引用其它供应商（渠道）的相同模型，
--         并按自己的口径定义不同的成本/售价折扣。
--         例如供应商 A 的产品明细中可以同时挂：
--           B 的 gpt-4o（折扣 0.85）与 C 的 gpt-4o（折扣 0.90）。
--   为此在 crm_supplier_project 上拆出两个供应商维度：
--     supplier_id          —— 这条报价归属哪个供应商（产品明细的维护主体，原字段，语义不变）
--     project_supplier_id  —— 这个模型取自哪个供应商的模型池（本次新增）
--   唯一键随之由 (supplier_id, project_id) 调整为
--   (supplier_id, project_supplier_id, project_id)。
-- 数据库：MySQL 5.7+ / 8.0
-- 幂等：可重复执行
-- ----------------------------

-- 删列/改索引不可逆，执行前建议备份：
-- create table crm_supplier_project_bak_20261119 as select * from crm_supplier_project;

-- ----------------------------
-- 1、新增列 project_supplier_id
-- ----------------------------
set @has_column = (
  select count(*) from information_schema.columns
   where table_schema = database()
     and table_name = 'crm_supplier_project'
     and column_name = 'project_supplier_id'
);
set @ddl = if(@has_column = 0,
  'alter table crm_supplier_project add column project_supplier_id int(11) default null comment ''模型所属供应商ID（模型池来源渠道）'' after project_id',
  'select ''column project_supplier_id already exists'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 2、回填历史数据：存量关联的模型都取自当前供应商自身
-- ----------------------------
update crm_supplier_project
   set project_supplier_id = supplier_id
 where project_supplier_id is null;

-- ----------------------------
-- 3、唯一键调整
--    旧：uk_supplier_project (supplier_id, project_id)
--    新：uk_supplier_project (supplier_id, project_supplier_id, project_id)
--    索引名保持不变，避免上层脚本/文档引用失效；
--    若索引已含 project_supplier_id 则跳过，重复执行安全。
-- ----------------------------
set @index_migrated = (
  select count(*) from information_schema.statistics
   where table_schema = database()
     and table_name = 'crm_supplier_project'
     and index_name = 'uk_supplier_project'
     and column_name = 'project_supplier_id'
);

-- 3.1 旧索引存在时先删除（不存在时执行空语句）
set @index_exists = (
  select count(*) from information_schema.statistics
   where table_schema = database()
     and table_name = 'crm_supplier_project'
     and index_name = 'uk_supplier_project'
);
set @ddl = if(@index_migrated = 0 and @index_exists > 0,
  'alter table crm_supplier_project drop index uk_supplier_project',
  'select ''no legacy index to drop'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- 3.2 建立三列唯一索引
set @ddl = if(@index_migrated = 0,
  'alter table crm_supplier_project add unique key uk_supplier_project (supplier_id, project_supplier_id, project_id)',
  'select ''uk_supplier_project already covers project_supplier_id'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 4、索引补充：按模型所属供应商检索（渠道模型池反查）
-- ----------------------------
set @has_index = (
  select count(*) from information_schema.statistics
   where table_schema = database()
     and table_name = 'crm_supplier_project'
     and index_name = 'idx_supplier_project_project_supplier'
);
set @ddl = if(@has_index = 0,
  'alter table crm_supplier_project add key idx_supplier_project_project_supplier (project_supplier_id)',
  'select ''idx_supplier_project_project_supplier already exists'' as msg');
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

-- ----------------------------
-- 5、变更前检查（手工执行，确认无重复后再建唯一索引）
-- ----------------------------
-- select supplier_id, project_supplier_id, project_id, count(*) as cnt
--   from crm_supplier_project
--  group by supplier_id, project_supplier_id, project_id
-- having count(*) > 1;
