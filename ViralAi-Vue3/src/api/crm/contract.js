import request from '@/utils/request'

// 查询合同分页列表
export function listContract(query) {
  return request({
    url: '/crm/contract/list',
    method: 'get',
    params: query
  })
}

// 查询合同详情（主信息 + 明细 + 审批记录 + 操作日志）
export function getContract(id) {
  return request({
    url: '/crm/contract/' + id,
    method: 'get'
  })
}

// 新建合同初始化：按商机带出客户与渠道信息，此时不生成合同编号
export function initContract(busId) {
  return request({
    url: '/crm/contract/init',
    method: 'post',
    data: { busId }
  })
}

// 保存合同（id 为空新增并生成编号，id 非空为草稿更新）
export function saveContract(data) {
  return request({
    url: '/crm/contract/save',
    method: 'post',
    data: data
  })
}

// 提交审批
export function submitAudit(contractId) {
  return request({
    url: '/crm/contract/submit-audit',
    method: 'post',
    data: { contractId }
  })
}

// 撤回审批（审批中 → 草稿，仅创建人可撤回）
export function withdrawAudit(contractId) {
  return request({
    url: '/crm/contract/withdraw',
    method: 'post',
    data: { contractId }
  })
}

// 审批处理（1 通过 / 2 驳回）
export function auditContract(data) {
  return request({
    url: '/crm/contract/audit',
    method: 'post',
    data: data
  })
}

// 合同作废
export function cancelContract(data) {
  return request({
    url: '/crm/contract/cancel',
    method: 'post',
    data: data
  })
}

// 合同归档
export function archiveContract(contractId) {
  return request({
    url: '/crm/contract/archive',
    method: 'post',
    data: { contractId }
  })
}

// 删除草稿合同
export function delContract(id) {
  return request({
    url: '/crm/contract/' + id,
    method: 'delete'
  })
}

// 查询合同模型明细
export function listContractProduct(contractId) {
  return request({
    url: '/crm/contract-product/list/' + contractId,
    method: 'get'
  })
}

// 明细批量保存（全量替换）
export function batchSaveContractProduct(data) {
  return request({
    url: '/crm/contract-product/batch-save',
    method: 'post',
    data: data
  })
}

// 删除单条明细
export function delContractProduct(id) {
  return request({
    url: '/crm/contract-product/' + id,
    method: 'delete'
  })
}

// 查询渠道可用模型（合同明细下拉唯一数据源）
export function listProductBySupplier(supplierId) {
  return request({
    url: '/crm/product/list-by-supplier',
    method: 'get',
    params: { supplierId }
  })
}
