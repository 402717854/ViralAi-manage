import request from '@/utils/request'

// 查询商机列表
export function listBusiness(query) {
  return request({
    url: '/crm/business/list',
    method: 'get',
    params: query
  })
}

// 查询商机详情（主信息 + 意向产品 + 关联合同 + 操作日志 + 可执行动作）
export function getBusiness(id) {
  return request({
    url: '/crm/business/' + id,
    method: 'get'
  })
}

// 保存商机（id 为空新增，非空编辑）
export function saveBusiness(data) {
  return request({
    url: '/crm/business/save',
    method: 'post',
    data: data
  })
}

// 商机状态流转
export function changeBusinessStatus(data) {
  return request({
    url: '/crm/business/update-status',
    method: 'post',
    data: data
  })
}

// 成交商机一键转合同，返回新建合同ID
export function convertContract(busId) {
  return request({
    url: '/crm/business/convert-contract',
    method: 'post',
    data: { id: busId }
  })
}

// 删除商机
export function delBusiness(ids) {
  return request({
    url: '/crm/business/' + ids,
    method: 'delete'
  })
}

// 查询商机意向产品清单
export function listBusinessProduct(busId) {
  return request({
    url: '/crm/business-product/list/' + busId,
    method: 'get'
  })
}

// 批量保存商机意向产品（全量替换）
export function batchSaveBusinessProduct(data) {
  return request({
    url: '/crm/business-product/batch-save',
    method: 'post',
    data: data
  })
}

// 删除单条商机意向产品（同一模型可能挂在多个渠道下，需带渠道定位）
export function delBusinessProduct(busId, supplierId, productId) {
  return request({
    url: '/crm/business-product',
    method: 'delete',
    params: { busId, supplierId, productId }
  })
}

// 查询渠道可用模型（与合同模块共用同一数据源）
export function listProductBySupplier(supplierId) {
  return request({
    url: '/crm/product/list-by-supplier',
    method: 'get',
    params: { supplierId }
  })
}
