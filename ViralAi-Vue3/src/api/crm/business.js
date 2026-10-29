import request from '@/utils/request'

// 查询CRM 商机列表
export function listBusiness(query) {
  return request({
    url: '/crm/business/list',
    method: 'get',
    params: query
  })
}

// 查询CRM 商机详细
export function getBusiness(id) {
  return request({
    url: '/crm/business/' + id,
    method: 'get'
  })
}

// 新增CRM 商机
export function addBusiness(data) {
  return request({
    url: '/crm/business',
    method: 'post',
    data: data
  })
}

// 修改CRM 商机
export function updateBusiness(data) {
  return request({
    url: '/crm/business',
    method: 'put',
    data: data
  })
}

// 查询商机当前允许流转的目标状态
export function getBusinessStatusOption(id) {
  return request({
    url: '/crm/business/statusOption/' + id,
    method: 'get'
  })
}

// 商机状态流转
export function changeBusinessStatus(data) {
  return request({
    url: '/crm/business/changeStatus',
    method: 'put',
    data: data
  })
}

// 删除CRM 商机
export function delBusiness(id) {
  return request({
    url: '/crm/business/' + id,
    method: 'delete'
  })
}
