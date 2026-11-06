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

// 渠道对账汇总
export function getSupplierStat(billCycle) {
  return request({
    url: '/crm/usage/supplier-stat',
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
