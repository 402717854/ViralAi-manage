package com.ruoyi.web.controller.crm;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.dto.GatewayCallLogUploadDto;
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
 * POST   /crm/gateway-call-log/upload   手工上传流水文件（Excel，call_no 幂等）
 * POST   /crm/gateway-call-log/upload-template 下载上传模板
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
    private static final Logger log = LoggerFactory.getLogger(CrmGatewayCallLogController.class);

    @Autowired
    private IGatewayCallLogService gatewayCallLogService;

    @Autowired
    private ICrmContractUsageService crmContractUsageService;

    /**
     * 查询流水列表
     *
     * <p>未显式指定调用时间区间时，默认只查「前一天」的流水：日常排查与导出都以昨日为准，
     * 同时避免误拉全表。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:list')")
    @GetMapping("/list")
    public TableDataInfo list(GatewayCallLogDto query)
    {
        applyDefaultCallTimeRange(query);
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
                autoReconcile(result, min, max, null);
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
     * 手工上传网关原始流水文件
     *
     * <p>供「拉取方式 = 文件上传」的渠道账户使用：上游不给接口或不允许接口拉取时，
     * 由财务导出流水后上传 Excel。客户由界面指定（一个文件归属一个客户，文件内不再带客户标识），
     * 模型编码按 {@code crm_gateway_pull_map} 解析；{@code call_no} 幂等，
     * 重复上传只会计入重复数，不会重复计费。</p>
     *
     * @param file 流水文件（xls / xlsx，表头见上传模板）
     * @param supplierId 流水所属渠道，必填
     * @param customerId 流水归属客户，必填
     * @param supplierAccountId 渠道账户，必填；客户 / 渠道 / 账户需为该客户合同明细里的组合
     * @param autoReconcile 入库后是否按文件内调用时间区间立即对账
     * @return 统计结果，msg 为可直接展示的中文摘要
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:ingest')")
    @RepeatSubmit
    @Log(title = "网关流水上传", businessType = BusinessType.IMPORT)
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("supplierId") Long supplierId,
                             @RequestParam("customerId") Long customerId,
                             @RequestParam("supplierAccountId") Long supplierAccountId,
                             @RequestParam(value = "autoReconcile", defaultValue = "true") boolean autoReconcile)
    {
        if (file == null || file.isEmpty())
        {
            return error("请选择要上传的流水文件");
        }
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".xls") && !lower.endsWith(".xlsx"))
        {
            return error("仅支持 xls / xlsx 格式的流水文件");
        }
        List<GatewayCallLogUploadDto> rows;
        try
        {
            ExcelUtil<GatewayCallLogUploadDto> util = new ExcelUtil<GatewayCallLogUploadDto>(
                    GatewayCallLogUploadDto.class);
            rows = util.importExcel(file.getInputStream());
        }
        catch (Exception e)
        {
            log.error("流水文件解析失败，文件={}", fileName, e);
            return error("文件解析失败，请使用「下载模板」提供的表头（第一行为表头，数据从第二行开始）："
                    + e.getMessage());
        }
        Map<String, Object> result = gatewayCallLogService.uploadAndIngest(rows, supplierId, supplierAccountId,
                customerId, fileName, getUsername());
        if (autoReconcile)
        {
            autoReconcile(result, (Date) result.get("startTime"), (Date) result.get("endTime"), supplierId);
        }
        AjaxResult ajax = AjaxResult.success(buildUploadMessage(result));
        ajax.put(AjaxResult.DATA_TAG, result);
        return ajax;
    }

    /**
     * 下载流水上传模板
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:ingest')")
    @PostMapping("/upload-template")
    public void uploadTemplate(HttpServletResponse response)
    {
        ExcelUtil<GatewayCallLogUploadDto> util = new ExcelUtil<GatewayCallLogUploadDto>(
                GatewayCallLogUploadDto.class);
        util.importTemplateExcel(response, "网关原始流水");
    }

    /**
     * 入库后按调用时间区间立即对账
     *
     * @param result 待回填的结果
     * @param start 起始时间
     * @param end 结束时间
     * @param supplierId 渠道ID，为空表示不限渠道（网关上报场景，流水可能跨渠道）
     */
    private void autoReconcile(Map<String, Object> result, Date start, Date end, Long supplierId)
    {
        if (start == null || end == null)
        {
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        CrmUsageReconcileVo vo = new CrmUsageReconcileVo();
        vo.setStartTime(format.format(start));
        vo.setEndTime(format.format(end));
        vo.setSupplierId(supplierId);
        result.put("reconcile", crmContractUsageService.reconcile(vo));
    }

    /**
     * 上传结果摘要
     *
     * <p>只把有值的信息拼进摘要，避免运维看到一长串「0 条」。</p>
     *
     * @param result 上传结果
     * @return 中文摘要
     */
    private String buildUploadMessage(Map<String, Object> result)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("上传完成：解析 ").append(value(result, "parsed")).append(" 行，入库 ")
                .append(value(result, "inserted")).append(" 条，重复跳过 ").append(value(result, "duplicated"))
                .append(" 条");
        int invalid = value(result, "invalid");
        if (invalid > 0)
        {
            msg.append("，无效 ").append(invalid).append(" 条");
        }
        int missingModel = value(result, "missingModel");
        if (missingModel > 0)
        {
            msg.append("，缺模型编码 ").append(missingModel).append(" 条");
        }
        return msg.toString();
    }

    /**
     * 安全取整型计数
     *
     * @param result 结果
     * @param key 键
     * @return 数值，缺失按 0
     */
    private int value(Map<String, Object> result, String key)
    {
        Object v = result.get(key);
        return v instanceof Number ? ((Number) v).intValue() : 0;
    }

    /**
     * 导出流水
     *
     * <p>与列表同口径：未显式指定调用时间区间时默认导出「前一天」，不允许无区间全量导出。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @Log(title = "网关流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, GatewayCallLogDto query)
    {
        applyDefaultCallTimeRange(query);
        List<GatewayCallLogDto> list = gatewayCallLogService.selectGatewayCallLogList(query);
        ExcelUtil<GatewayCallLogDto> util = new ExcelUtil<GatewayCallLogDto>(GatewayCallLogDto.class);
        util.exportExcel(response, list, "网关调用流水");
    }

    /**
     * 列表与导出的默认调用时间区间：前一天（昨日 00:00:00 ~ 23:59:59）
     *
     * <p>调用时间区间两端都齐全时才按入参查询；任一端缺失都回落到「前一天」，
     * 避免出现无上界/无下界的全量扫描。前端日期控件同样默认且不可清空。</p>
     *
     * @param query 查询条件，入参可能为 null
     */
    private void applyDefaultCallTimeRange(GatewayCallLogDto query)
    {
        if (query == null)
        {
            return;
        }
        Map<String, Object> params = query.getParams();
        if (params == null)
        {
            params = new HashMap<>();
            query.setParams(params);
        }
        Object begin = params.get("beginTime");
        Object end = params.get("endTime");
        boolean beginBlank = begin == null || StringUtils.isEmpty(String.valueOf(begin));
        boolean endBlank = end == null || StringUtils.isEmpty(String.valueOf(end));
        if (!beginBlank && !endBlank)
        {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        String day = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, calendar.getTime());
        params.put("beginTime", day);
        params.put("endTime", day);
    }
}
