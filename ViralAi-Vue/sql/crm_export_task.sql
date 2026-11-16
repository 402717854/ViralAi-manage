-- ============================================================
-- 异步导出任务（网关原始流水 / 对账台账）
--
-- 背景：
--   两个导出接口此前是同步导出：数据量大时前端 10s 就超时中断，请求虽然还在
--   服务端跑，但用户已经拿不到结果；把超时放宽到 5 分钟也只是让用户干等，
--   并且长时间占用 Web 线程。改为「提交任务 → 后台生成文件 → 前端轮询 → 下载」。
--
-- 设计要点：
--   1. 查询条件以 JSON 固化在 query_params，其中包含提交时算好的数据权限范围
--      （params.scopeSupplierIds）。异步线程没有登录上下文，若不在提交线程固化，
--      数据权限会退化成「不过滤」，这是本表最关键的安全约束。
--   2. 文件落盘到 RuoYi 的 download 目录（profile/download/），下载统一走带鉴权的
--      接口，不把导出文件暴露成匿名可访问的静态资源。
--   3. 任务与文件默认保留 7 天，每次提交新任务时顺带清理过期记录与文件。
--
-- 本脚本幂等，可重复执行；MySQL 5.7+ / 8.0
-- ============================================================

create table if not exists crm_export_task (
  id            bigint       not null auto_increment comment '主键',
  biz_type      varchar(32)  not null                comment '业务类型：gateway-log 网关原始流水 / usage 对账台账',
  file_name     varchar(200) not null                comment '下载时展示的文件名（含扩展名）',
  file_path     varchar(500)          default null   comment '服务器落盘绝对路径',
  query_params  text                  default null   comment '提交时的查询条件 JSON（含数据权限范围）',
  status        tinyint      not null default 0      comment '状态：0待处理 1处理中 2成功 3失败',
  row_count     int          not null default 0      comment '导出行数',
  err_msg       varchar(1000)         default null   comment '失败原因',
  creator_id    bigint                default null   comment '提交人ID',
  creator_name  varchar(64)           default null   comment '提交人账号',
  create_time   datetime              default null   comment '提交时间',
  start_time    datetime              default null   comment '开始执行时间',
  finish_time   datetime              default null   comment '完成时间',
  expire_time   datetime              default null   comment '文件过期时间',
  deleted       tinyint      not null default 0      comment '是否删除：0否 1是',
  primary key (id),
  key idx_export_task_creator (creator_id, create_time),
  key idx_export_task_status (status),
  key idx_export_task_expire (expire_time)
) engine=innodb default charset=utf8mb4 comment='异步导出任务（流水/台账）';
