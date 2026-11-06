import request from '@/utils/request'

// 查询渠道账户列表（apiKey 已脱敏）
export function listSupplierAccount(query) {
  return request({
    url: '/crm/supplier-account/list',
    method: 'get',
    params: query
  })
}

// 查询渠道账户详情（返回完整 apiKey，供编辑回填）
export function getSupplierAccount(id) {
  return request({
    url: '/crm/supplier-account/' + id,
    method: 'get'
  })
}

// 新增渠道账户
export function addSupplierAccount(data) {
  return request({
    url: '/crm/supplier-account',
    method: 'post',
    data: data
  })
}

// 修改渠道账户
export function updateSupplierAccount(data) {
  return request({
    url: '/crm/supplier-account',
    method: 'put',
    data: data
  })
}
