package com.ruoyi.crm.service;

import jakarta.servlet.http.HttpServletResponse;

import com.ruoyi.crm.domain.CrmExportTask;
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.GatewayCallLogDto;

/**
 * 异步导出任务Service接口
 *
 * <p>网关原始流水与对账台账的导出一律走「提交任务 → 后台生成文件 → 前端轮询 → 下载」，
 * 避免同步导出在前端超时、长时间占用 Web 线程。</p>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
public interface ICrmExportTaskService
{
    /** 业务类型：网关原始流水 */
    String BIZ_GATEWAY_LOG = "gateway-log";

    /** 业务类型：对账台账 */
    String BIZ_USAGE = "usage";

    /**
     * 提交网关原始流水导出任务
     *
     * <p>提交时会把当前账号的数据权限范围固化进查询条件：异步线程没有登录上下文，
     * 若不在提交线程算好，权限会退化成不过滤。</p>
     *
     * @param query 页面查询条件（可空，空表示不额外过滤）
     * @return 任务ID
     */
    Long submitGatewayLogExport(GatewayCallLogDto query);

    /**
     * 提交对账台账导出任务
     *
     * @param query 页面查询条件（可空）
     * @return 任务ID
     */
    Long submitUsageExport(CrmContractUsageDto query);

    /**
     * 查询任务（仅限本人，管理员不限）
     *
     * @param id 任务ID
     * @return 任务
     */
    CrmExportTask selectTask(Long id);

    /**
     * 下载导出文件
     *
     * @param id 任务ID
     * @param response HTTP 响应
     */
    void download(Long id, HttpServletResponse response);
}
