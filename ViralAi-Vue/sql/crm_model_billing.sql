-- ----------------------------
-- 模型计费（主表 + 价格明细子表）
-- 对应接口结构：modelCode / modelName / currency / exchangeRate / discountRate /
--              timeZone / 各类 token 单价 / mediaPrices / imagePrices / videoPrices / timeRangePrices
-- 数据库：MySQL 5.7+ / 8.0
-- ----------------------------

-- ----------------------------
-- 1、模型计费主表
-- ----------------------------
drop table if exists crm_model_billing;
create table crm_model_billing (
  id                                  bigint(20)      not null auto_increment    comment '主键',
  model_code                          varchar(128)    not null                   comment '模型编码',
  model_name                          varchar(128)    default null               comment '模型名称',
  currency                            varchar(8)      default 'USD'              comment '结算币种，如 USD / CNY/积分',
  exchange_rate                       decimal(18,8)   default null               comment '汇率，1 人民币可兑换的外币数量（如 USD 填 0.1446，CNY 填 1）',
  token_unit_quantity                 bigint(20)      default 1000000            comment '主表 token 单价的计价单位数量，默认每百万 token',
  time_zone                           varchar(64)     default 'UTC'              comment '计费时区，如 UTC / Asia/Shanghai',

  status                              tinyint(1)      default 1                  comment '状态 0失效 1有效',
  remark                              varchar(500)    default null               comment '备注',
  creator                             bigint(20)      default null               comment '创建者',
  create_time                         datetime        default null               comment '创建时间',
  updater                             bigint(20)      default null               comment '更新者',
  update_time                         datetime        default null               comment '更新时间',
  deleted                             tinyint(1)      default 0                  comment '是否删除 0否 1是',
  tenant_id                           bigint(20)      default null               comment '租户ID',
  primary key (id),
  key idx_model_billing_code (model_code, deleted),
  key idx_model_billing_status (status, deleted)
) engine=innodb auto_increment=1 comment = '模型计费表';

-- 若表已创建，仅补计费单位列时执行（幂等性由执行前判断，重复执行会报 Duplicate column）：
-- alter table crm_model_billing add column token_unit_quantity bigint(20) default 1000000 comment '主表 token 单价的计价单位数量，默认每百万 token' after discount_rate;

-- ----------------------------
-- 2、模型计费价格明细表
--    统一承载 mediaPrices / imagePrices / videoPrices / timeRangePrices
--    四类结构通过 price_type 区分，公共字段（billingMetric/unitQuantity/officialUnitPrice…）共用列，
--    差异化字段（resolutionTier、inputVideoMode、时段）以可空列承载，仍无法覆盖的以 ext_json 兜底
-- ----------------------------
drop table if exists crm_model_billing_price;
create table crm_model_billing_price (
  id                    bigint(20)      not null auto_increment    comment '主键',
  billing_id            bigint(20)      not null                   comment '模型计费主表ID',
  billing_type          int(4)          not null                   comment '计费类型 空闲/高峰/全天',
  media_type            varchar(16)     default null               comment '媒体类型 TEXT/IMAGE/AUDIO/VIDEO',
  billing_metric        varchar(32)     default null               comment '计费指标 TOKEN/CALL/SECOND/IMAGE/VIDEO',
  unit_quantity         bigint(20)      default null               comment '计价单位数量，如 1000000 表示每百万 token',
  official_unit_price   decimal(18,8)   default null               comment '官方单价',
  currency              varchar(8)      default null               comment '币种',
  resolution_tier       varchar(32)     default null               comment '分辨率档位 360P/480P/720P/1080P/4K',
  input_video_mode      varchar(32)     default null               comment '视频输入模式 WITH_VIDEO/WITHOUT_VIDEO',
  start_time            varchar(16)     default null               comment '折扣计费时段开始，格式 HH:mm',
  end_time              varchar(16)     default null               comment '折扣计费时段结束，格式 HH:mm',
  discount_rate                       decimal(10,4)   default 1.0000             comment '折扣率，1 表示不打折',
  input_token_price                   decimal(18,8)   default null               comment '输入 token 单价',
  output_token_price                  decimal(18,8)   default null               comment '输出 token 单价',

  input_cached_token_price            decimal(18,8)   default null               comment '输入缓存 token 单价',
  output_cached_token_price           decimal(18,8)   default null               comment '输出缓存 token 单价',
  cache_read_token_price              decimal(18,8)   default null               comment '缓存读取 token 单价',
  cache_write_token_price             decimal(18,8)   default null               comment '缓存写入 token 单价',
  cache_creation_input_tokens_price   decimal(18,8)   default null               comment '缓存创建输入 token 单价',
  cache_read_token_price_above200k    decimal(18,8)   default null               comment '缓存读取 token 单价（上下文超 200K 档）',
  ephemeral5m_token_price             decimal(18,8)   default null               comment '5 分钟临时缓存 token 单价',
  ephemeral1h_token_price             decimal(18,8)   default null               comment '1 小时临时缓存 token 单价',

  thoughts_token_price                decimal(18,8)   default null               comment '思考（reasoning）token 单价',
  sort_order            int(4)          default 0                  comment '排序号',
  ext_json              varchar(2000)   default null               comment '扩展属性（JSON），承载未建模字段',
  remark                varchar(255)    default null               comment '备注',
  creator               bigint(20)      default null               comment '创建者',
  create_time           datetime        default null               comment '创建时间',
  updater               bigint(20)      default null               comment '更新者',
  update_time           datetime        default null               comment '更新时间',
  deleted               tinyint(1)      default 0                  comment '是否删除 0否 1是',
  primary key (id),
  key idx_billing_price_billing (billing_id, price_type)
) engine=innodb auto_increment=1 comment = '模型计费价格明细表';

-- ----------------------------
-- 3、菜单与按钮权限（按需执行，role_id=1 为超管，可按实际角色调整）
-- ----------------------------
-- insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
-- values('模型计费', 0, 5, 'billing', 'crm/billing/index', 1, 0, 'C', '0', '0', 'crm:billing:list', 'money', 'admin', sysdate(), '模型计费菜单');

-- ----------------------------
-- 4、字典数据（幂等，可重复执行；本节可单独执行，不依赖建表）
--    字典值必须与 CrmModelBillingPrice 中的 PRICE_TYPE_* 常量保持一致
--    前端用法：const { crm_billing_price_type } = proxy.useDict('crm_billing_price_type')
-- ----------------------------

-- 4.1 字典类型
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '计费价格类型', 'crm_billing_price_type', '0', 'admin', sysdate(), '模型计费-价格明细归属类型'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_billing_price_type');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '计费媒体类型', 'crm_billing_media_type', '0', 'admin', sysdate(), '模型计费-媒体类型'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_billing_media_type');

insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '计费指标', 'crm_billing_metric', '0', 'admin', sysdate(), '模型计费-计费指标'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_billing_metric');

-- 4.2 价格类型 crm_billing_price_type
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '媒体价格', 'MEDIA', 'crm_billing_price_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '按分辨率与输入模式计价的媒体单价，对应 mediaPrices'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_price_type' and dict_value = 'MEDIA');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '图片价格', 'IMAGE', 'crm_billing_price_type', '', 'success', 'N', '0', 'admin', sysdate(), '图片单价，对应 imagePrices'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_price_type' and dict_value = 'IMAGE');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '视频价格', 'VIDEO', 'crm_billing_price_type', '', 'warning', 'N', '0', 'admin', sysdate(), '视频单价，对应 videoPrices'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_price_type' and dict_value = 'VIDEO');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '时段价格', 'TIME_RANGE', 'crm_billing_price_type', '', 'info', 'N', '0', 'admin', sysdate(), '分时段差异化单价，对应 timeRangePrices'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_price_type' and dict_value = 'TIME_RANGE');

-- 4.3 媒体类型 crm_billing_media_type
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '音频', 'AUDIO', 'crm_billing_media_type', '', 'info', 'N', '0', 'admin', sysdate(), '音频模态'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_media_type' and dict_value = 'AUDIO');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '视频', 'VIDEO', 'crm_billing_media_type', '', 'warning', 'Y', '0', 'admin', sysdate(), '视频模态，视频类价格默认值'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_media_type' and dict_value = 'VIDEO');

-- 4.4 计费指标 crm_billing_metric
insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '按 Token', 'TOKEN', 'crm_billing_metric', '', 'primary', 'Y', '0', 'admin', sysdate(), '按 token 用量计费'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_metric' and dict_value = 'TOKEN');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '按次', 'CALL', 'crm_billing_metric', '', 'success', 'N', '0', 'admin', sysdate(), '按调用次数计费'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_metric' and dict_value = 'CALL');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '按秒', 'SECOND', 'crm_billing_metric', '', 'info', 'N', '0', 'admin', sysdate(), '按时长（秒）计费'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_metric' and dict_value = 'SECOND');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5, '按条视频', 'VIDEO', 'crm_billing_metric', '', 'danger', 'N', '0', 'admin', sysdate(), '按生成视频条数计费'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_metric' and dict_value = 'VIDEO');

-- 4.5 分辨率档位 crm_billing_resolution
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '计费分辨率档位', 'crm_billing_resolution', '0', 'admin', sysdate(), '模型计费-分辨率档位'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_billing_resolution');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '480P', '480P', 'crm_billing_resolution', '', 'info', 'N', '0', 'admin', sysdate(), '标清 480P'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_resolution' and dict_value = '480P');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '720P', '720P', 'crm_billing_resolution', '', 'info', 'N', '0', 'admin', sysdate(), '高清 720P'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_resolution' and dict_value = '720P');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3, '1080P', '1080P', 'crm_billing_resolution', '', 'primary', 'Y', '0', 'admin', sysdate(), '全高清 1080P'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_resolution' and dict_value = '1080P');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4, '4K', '4K', 'crm_billing_resolution', '', 'warning', 'N', '0', 'admin', sysdate(), '超高清 4K'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_resolution' and dict_value = '4K');

-- 4.6 视频输入模式 crm_billing_video_mode
insert into sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
select '计费视频输入模式', 'crm_billing_video_mode', '0', 'admin', sysdate(), '模型计费-视频输入模式'
from dual where not exists (select 1 from sys_dict_type where dict_type = 'crm_billing_video_mode');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1, '不含视频输入', 'WITHOUT_VIDEO', 'crm_billing_video_mode', '', 'primary', 'Y', '0', 'admin', sysdate(), '文生视频，无视频参考输入'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_video_mode' and dict_value = 'WITHOUT_VIDEO');

insert into sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2, '含视频输入', 'WITH_VIDEO', 'crm_billing_video_mode', '', 'success', 'N', '0', 'admin', sysdate(), '图生/视频生视频，含视频参考输入'
from dual where not exists (select 1 from sys_dict_data where dict_type = 'crm_billing_video_mode' and dict_value = 'WITH_VIDEO');
