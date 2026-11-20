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

// 查询渠道可售模型（启用渠道 × 渠道已上架模型），商机/合同明细的模型选择数据源
export function getChannelProductOptions() {
  return request({
    url: '/crm/supplier/channel-product-options',
    method: 'get'
  })
}

// 查询渠道账户选项（不含 apiKey 与密码），商机/合同明细的渠道账户选择数据源
export function getChannelAccountOptions() {
  return request({
    url: '/crm/supplier/channel-account-options',
    method: 'get'
  })
}

// 按模型ID查询报过价的渠道（含渠道名称与折扣区间），供应商报价明细级联所属渠道用
export function getProjectChannels(projectId) {
  return request({
    url: '/crm/supplier/project-channels',
    method: 'get',
    params: { projectId }
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

// 按供应商ID查询其渠道账户列表（apiKey 已脱敏，不含账户密码）
export function getSupplierAccounts(id) {
  return request({
    url: '/crm/supplier/' + id + '/accounts',
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
