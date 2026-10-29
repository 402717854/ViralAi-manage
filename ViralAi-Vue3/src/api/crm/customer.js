import request from '@/utils/request'

// 查询CRM 客户列表
export function listCustomer(query) {
  return request({
    url: '/crm/customer/list',
    method: 'get',
    params: query
  })
}

// 查询CRM 客户下拉选项（返回 id、name 两个字段）
export function getCustomerOptions() {
  return request({
    url: '/crm/customer/options',
    method: 'get'
  })
}

// 查询CRM 客户详细
export function getCustomer(id) {
  return request({
    url: '/crm/customer/' + id,
    method: 'get'
  })
}

// 新增CRM 客户
export function addCustomer(data) {
  return request({
    url: '/crm/customer',
    method: 'post',
    data: data
  })
}

// 修改CRM 客户
export function updateCustomer(data) {
  return request({
    url: '/crm/customer',
    method: 'put',
    data: data
  })
}

// 删除CRM 客户
export function delCustomer(id) {
  return request({
    url: '/crm/customer/' + id,
    method: 'delete'
  })
}
