import request from '@/utils/request'

// 提交网关原始流水导出任务（异步，返回任务ID）
export function submitGatewayLogExport(data) {
  return request({
    url: '/crm/export/task/gateway-log',
    method: 'post',
    data: data
  })
}

// 提交对账台账导出任务（异步，返回任务ID）
export function submitUsageExport(data) {
  return request({
    url: '/crm/export/task/usage',
    method: 'post',
    data: data
  })
}

// 查询导出任务状态：0待处理 1处理中 2成功 3失败
export function getExportTask(id) {
  return request({
    url: '/crm/export/task/' + id,
    method: 'get'
  })
}

// 下载导出结果文件（blob）
export function downloadExportFile(id) {
  return request({
    url: '/crm/export/task/' + id + '/download',
    method: 'get',
    responseType: 'blob',
    // 大文件在网络慢时可能超过默认 10s
    timeout: 300000
  })
}
