import request from '@/utils/request'

// 查询对账台账分页列表
export function listUsage(query) {
  return request({
    url: '/crm/usage/list',
    method: 'get',
    params: query
  })
}

// 查询台账详情
export function getUsage(id) {
  return request({
    url: '/crm/usage/' + id,
    method: 'get'
  })
}

// 手动执行对账（区间 + 渠道）
export function reconcileUsage(data) {
  return request({
    url: '/crm/usage/reconcile',
    method: 'post',
    data: data
  })
}

// 单条重新匹配
export function rematchUsage(id) {
  return request({
    url: '/crm/usage/rematch/' + id,
    method: 'post'
  })
}

// 渠道对账看板
export function getUsageBoard(billCycle) {
  return request({
    url: '/crm/usage/board',
    method: 'get',
    params: { billCycle }
  })
}

// 用量趋势（近 N 期，默认 6 期；缺失账期后端已补 0）
export function getUsageTrend(billCycle, months = 6) {
  return request({
    url: '/crm/usage/board/trend',
    method: 'get',
    params: { billCycle, months }
  })
}

// 客户 + 渠道对账汇总（旧地址 /crm/usage/supplier-stat 仍保留为别名）
export function getCustomerSupplierStat(billCycle) {
  return request({
    url: '/crm/usage/customer-supplier-stat',
    method: 'get',
    params: { billCycle }
  })
}

// 查询网关原始流水
export function listCallLog(query) {
  return request({
    url: '/crm/gateway-call-log/list',
    method: 'get',
    params: query
  })
}

// 网关流水中转上报（联调 / 网关推送），autoReconcile 为 true 时入库后立即对账
export function ingestCallLog(data, autoReconcile = true) {
  return request({
    url: '/crm/gateway-call-log/ingest',
    method: 'post',
    params: { autoReconcile },
    data: data
  })
}

// 从网关拉取区间流水
export function pullCallLog(data) {
  return request({
    url: '/crm/gateway-call-log/pull',
    method: 'post',
    data: data
  })
}

// 手工上传网关原始流水文件（Excel），入库后按文件内区间自动对账
export function uploadCallLog(data) {
  return request({
    url: '/crm/gateway-call-log/upload',
    method: 'post',
    data: data,
    headers: { 'Content-Type': 'multipart/form-data' },
    // 文件解析 + 入库 + 对账耗时明显长于普通接口，单独放大超时
    timeout: 300000
  })
}

// 客户可用的「渠道 + 渠道账户」组合，用于上传弹窗三级联动
export function getCustomerSupplierAccounts(customerId) {
  return request({
    url: '/crm/usage/customer-supplier-accounts',
    method: 'get',
    params: { customerId }
  })
}
