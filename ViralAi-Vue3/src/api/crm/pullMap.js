import request from '@/utils/request'

// 查询流水映射列表（附带渠道、账户、客户、模型名称）
export function listPullMap(query) {
  return request({
    url: '/crm/pull-map/list',
    method: 'get',
    params: query
  })
}

// 查询流水映射详情
export function getPullMap(id) {
  return request({
    url: '/crm/pull-map/' + id,
    method: 'get'
  })
}

// 新增流水映射
export function addPullMap(data) {
  return request({
    url: '/crm/pull-map',
    method: 'post',
    data: data
  })
}

// 修改流水映射
export function updatePullMap(data) {
  return request({
    url: '/crm/pull-map',
    method: 'put',
    data: data
  })
}

// 删除流水映射
export function delPullMap(id) {
  return request({
    url: '/crm/pull-map/' + id,
    method: 'delete'
  })
}
