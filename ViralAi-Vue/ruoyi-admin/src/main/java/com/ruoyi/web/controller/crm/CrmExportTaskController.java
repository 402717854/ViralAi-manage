package com.ruoyi.web.controller.crm;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.domain.CrmExportTask;
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.service.ICrmExportTaskService;

/**
 * 异步导出任务Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * POST   /crm/export/task/gateway-log      提交网关原始流水导出任务
 * POST   /crm/export/task/usage            提交对账台账导出任务
 * GET    /crm/export/task/{id}             查询任务状态
 * GET    /crm/export/task/{id}/download    下载结果文件
 * </pre>
 *
 * <p>提交与下载都要求 {@code crm:usage:export} 权限；任务仅创建人本人（或管理员）可见可下载。</p>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
@RestController
@RequestMapping("/crm/export/task")
public class CrmExportTaskController extends BaseController
{
    @Autowired
    private ICrmExportTaskService crmExportTaskService;

    /**
     * 提交网关原始流水导出任务
     *
     * <p>查询条件与当前账号的数据权限范围会在提交时固化，后台线程据此生成文件，
     * 前端拿到任务ID后轮询状态，成功后调用下载接口。</p>
     *
     * @param query 页面查询条件（可空）
     * @return 任务ID
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @RepeatSubmit
    @Log(title = "网关流水导出", businessType = BusinessType.EXPORT)
    @PostMapping("/gateway-log")
    public AjaxResult submitGatewayLog(@RequestBody(required = false) GatewayCallLogDto query)
    {
        Long taskId = crmExportTaskService.submitGatewayLogExport(query);
        return AjaxResult.success("导出任务已提交，生成完成后会自动下载", taskId);
    }

    /**
     * 提交对账台账导出任务
     *
     * @param query 页面查询条件（可空）
     * @return 任务ID
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @RepeatSubmit
    @Log(title = "对账台账导出", businessType = BusinessType.EXPORT)
    @PostMapping("/usage")
    public AjaxResult submitUsage(@RequestBody(required = false) CrmContractUsageDto query)
    {
        Long taskId = crmExportTaskService.submitUsageExport(query);
        return AjaxResult.success("导出任务已提交，生成完成后会自动下载", taskId);
    }

    /**
     * 查询任务状态
     *
     * <p>状态：0待处理 1处理中 2成功 3失败；成功后前端凭 id 调用下载接口。
     * 查询条件 JSON 体积大且前端无用，这里置空不返回。</p>
     *
     * @param id 任务ID
     * @return 任务
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        CrmExportTask task = crmExportTaskService.selectTask(id);
        task.setQueryParams(null);
        return success(task);
    }

    /**
     * 下载导出文件
     *
     * @param id 任务ID
     * @param response HTTP 响应
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @GetMapping("/{id}/download")
    public void download(@PathVariable("id") Long id, HttpServletResponse response)
    {
        crmExportTaskService.download(id, response);
    }
}
