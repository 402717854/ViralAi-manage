import request from '@/utils/request'

// 查询CRM模型产品列表
export function listProduct(query) {
  return request({
    url: '/crm/product/list',
    method: 'get',
    params: query
  })
}

// 查询CRM模型产品详细
export function getProduct(id) {
  return request({
    url: '/crm/product/' + id,
    method: 'get'
  })
}

// 查询CRM模型产品全部数据（下拉框用）
export function listAllProduct() {
  return request({
    url: '/crm/product/listAll',
    method: 'get'
  })
}

// 新增CRM模型产品
export function addProduct(data) {
  return request({
    url: '/crm/product',
    method: 'post',
    data: data
  })
}

// 修改CRM模型产品
export function updateProduct(data) {
  return request({
    url: '/crm/product',
    method: 'put',
    data: data
  })
}

// 删除CRM模型产品
export function delProduct(id) {
  return request({
    url: '/crm/product/' + id,
    method: 'delete'
  })
}
