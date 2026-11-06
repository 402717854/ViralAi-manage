package com.ruoyi.web.controller.crm;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.service.ICrmContractUsageService;
import com.ruoyi.crm.service.IGatewayCallLogService;

/**
 * 网关调用流水Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/gateway-call-log/list     流水列表（排查对账源数据）
 * POST   /crm/gateway-call-log/ingest   网关推送 / 联调上报（数组，call_no 幂等）
 * POST   /crm/gateway-call-log/pull     按区间从网关拉取
 * POST   /crm/gateway-call-log/export   导出流水
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/gateway-call-log")
public class CrmGatewayCallLogController extends BaseController
{
    @Autowired
    private IGatewayCallLogService gatewayCallLogService;

    @Autowired
    private ICrmContractUsageService crmContractUsageService;

    /**
     * 查询流水列表
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:list')")
    @GetMapping("/list")
    public TableDataInfo list(GatewayCallLogDto query)
    {
        startPage();
        List<GatewayCallLogDto> list = gatewayCallLogService.selectGatewayCallLogList(query);
        return getDataTable(list);
    }

    /**
     * 批量上报流水
     *
     * <p>按 call_no 幂等，重复推送计入 duplicated；autoReconcile 为 true（默认）时，
     * 入库后立即按上报数据的时间区间执行一次对账，便于联调时即时看到台账。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:ingest')")
    @RepeatSubmit
    @Log(title = "网关流水上报", businessType = BusinessType.INSERT)
    @PostMapping("/ingest")
    public AjaxResult ingest(@RequestBody List<GatewayCallLog> logs,
                             @RequestParam(value = "autoReconcile", defaultValue = "true") boolean autoReconcile)
    {
        Map<String, Object> result = gatewayCallLogService.ingest(logs);
        if (autoReconcile && logs != null && !logs.isEmpty())
        {
            Date min = null;
            Date max = null;
            for (GatewayCallLog item : logs)
            {
                if (item == null || item.getCallTime() == null)
                {
                    continue;
                }
                if (min == null || item.getCallTime().before(min))
                {
                    min = item.getCallTime();
                }
                if (max == null || item.getCallTime().after(max))
                {
                    max = item.getCallTime();
                }
            }
            if (min != null)
            {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                CrmUsageReconcileVo vo = new CrmUsageReconcileVo();
                vo.setStartTime(format.format(min));
                vo.setEndTime(format.format(max));
                result.put("reconcile", crmContractUsageService.reconcile(vo));
            }
        }
        return success(result);
    }

    /**
     * 按渠道 + 渠道账户拉取指定区间流水
     *
     * <p>只传 supplierAccountId 拉该账户；只传 supplierId 拉该渠道下所有启用账户；
     * 都不传拉全部启用账户。上游协议由账户的 pullChannel 决定
     * （viralaiai 中转站 / pixverse / 通用），详见 crm_gateway_pull.sql。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:reconcile')")
    @RepeatSubmit
    @Log(title = "网关流水拉取", businessType = BusinessType.INSERT)
    @PostMapping("/pull")
    public AjaxResult pull(@RequestBody CrmUsageReconcileVo vo)
    {
        return success(gatewayCallLogService.pullAndIngest(
                vo == null ? null : vo.getStartTime(),
                vo == null ? null : vo.getEndTime(),
                vo == null ? null : vo.getSupplierId(),
                vo == null ? null : vo.getSupplierAccountId()));
    }

    /**
     * 导出流水
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @Log(title = "网关流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GatewayCallLogDto query)
    {
        List<GatewayCallLogDto> list = gatewayCallLogService.selectGatewayCallLogList(query);
        ExcelUtil<GatewayCallLogDto> util = new ExcelUtil<GatewayCallLogDto>(GatewayCallLogDto.class);
        util.exportExcel(response, list, "网关调用流水");
    }
}
