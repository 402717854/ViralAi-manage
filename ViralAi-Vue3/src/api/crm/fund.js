import request from '@/utils/request'

// 客户资金列表（客户维度，含 0 余额客户）
export function listFundAccount(query) {
  return request({
    url: '/crm/fund/account/list',
    method: 'get',
    params: query
  })
}

// 账户详情
export function getFundAccount(customerId) {
  return request({
    url: '/crm/fund/account/' + customerId,
    method: 'get'
  })
}

// 顶部汇总
export function getFundSummary() {
  return request({
    url: '/crm/fund/summary',
    method: 'get'
  })
}

// 资金流水分页
export function listFundFlow(query) {
  return request({
    url: '/crm/fund/flow/list',
    method: 'get',
    params: query
  })
}

// 充值单分页
export function listFundRecharge(query) {
  return request({
    url: '/crm/fund/recharge/list',
    method: 'get',
    params: query
  })
}

// 充值登记
export function createFundRecharge(data) {
  return request({
    url: '/crm/fund/recharge',
    method: 'post',
    data: data
  })
}

// 充值审核（pass=true 入账）
export function auditFundRecharge(data) {
  return request({
    url: '/crm/fund/recharge/audit',
    method: 'post',
    data: data
  })
}

// 退款单分页
export function listFundRefund(query) {
  return request({
    url: '/crm/fund/refund/list',
    method: 'get',
    params: query
  })
}

// 退款申请
export function createFundRefund(data) {
  return request({
    url: '/crm/fund/refund',
    method: 'post',
    data: data
  })
}

// 退款审批
export function auditFundRefund(data) {
  return request({
    url: '/crm/fund/refund/audit',
    method: 'post',
    data: data
  })
}

// 人工调账（direction 1调增 2调减）
export function adjustFund(data) {
  return request({
    url: '/crm/fund/adjust',
    method: 'post',
    data: data
  })
}

// 期初余额批量导入
export function initFundBalance(data) {
  return request({
    url: '/crm/fund/init',
    method: 'post',
    data: data
  })
}

// 账实一致性校验（应返回空数组）
export function checkFund() {
  return request({
    url: '/crm/fund/check',
    method: 'get'
  })
}
