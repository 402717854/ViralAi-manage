import request from '@/utils/request'

// 查询对账账单分页列表
export function listBill(query) {
  return request({
    url: '/crm/bill/list',
    method: 'get',
    params: query
  })
}

// 查询账单详情（账单 + 用量明细 + 差异）
export function getBill(id) {
  return request({
    url: '/crm/bill/' + id,
    method: 'get'
  })
}

// 生成账单（渠道 + 账期）
export function generateBill(data) {
  return request({
    url: '/crm/bill/generate',
    method: 'post',
    data: data
  })
}

// 账单复核（2 已确认 / 4 差异挂起）
export function auditBill(data) {
  return request({
    url: '/crm/bill/audit',
    method: 'post',
    data: data
  })
}

// 账单结算
export function settleBill(id) {
  return request({
    url: '/crm/bill/settle/' + id,
    method: 'post'
  })
}

// 查询差异清单
export function listBillDiff(query) {
  return request({
    url: '/crm/bill/diff/list',
    method: 'get',
    params: query
  })
}

// 差异处理（1 调账 / 2 豁免 / 3 驳回）
export function handleBillDiff(data) {
  return request({
    url: '/crm/bill/diff/handle',
    method: 'post',
    data: data
  })
}
