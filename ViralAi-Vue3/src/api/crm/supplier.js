import request from '@/utils/request'

// 查询CRM 供应商列表
export function listSupplier(query) {
  return request({
    url: '/crm/supplier/list',
    method: 'get',
    params: query
  })
}

// 查询CRM 供应商下拉选项（返回 id、name 两个字段）
export function getSupplierOptions() {
  return request({
    url: '/crm/supplier/options',
    method: 'get'
  })
}

// 查询CRM 供应商详细
export function getSupplier(id) {
  return request({
    url: '/crm/supplier/' + id,
    method: 'get'
  })
}

// 按供应商ID查询其产品关联明细（含模型型号 projectNo 与模型厂商 projectName）
export function getSupplierProjects(id) {
  return request({
    url: '/crm/supplier/' + id + '/projects',
    method: 'get'
  })
}

// 新增CRM 供应商
export function addSupplier(data) {
  return request({
    url: '/crm/supplier',
    method: 'post',
    data: data
  })
}

// 修改CRM 供应商
export function updateSupplier(data) {
  return request({
    url: '/crm/supplier',
    method: 'put',
    data: data
  })
}

// 删除CRM 供应商
export function delSupplier(id) {
  return request({
    url: '/crm/supplier/' + id,
    method: 'delete'
  })
}
