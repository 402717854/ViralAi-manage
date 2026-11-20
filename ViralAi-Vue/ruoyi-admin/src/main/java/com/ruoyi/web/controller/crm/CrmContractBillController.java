package com.ruoyi.web.controller.crm;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
import com.ruoyi.crm.dto.CrmBillAuditVo;
import com.ruoyi.crm.dto.CrmBillDiffHandleVo;
import com.ruoyi.crm.dto.CrmBillGenerateVo;
import com.ruoyi.crm.dto.CrmBillSettleVo;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;
import com.ruoyi.crm.dto.CrmContractBillDto;
import com.ruoyi.crm.dto.CrmContractBillSubDto;
import com.ruoyi.crm.service.ICrmContractBillService;

/**
 * 对账账单Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/bill/list          账单分页
 * GET    /crm/bill/sub/list      子账单分页（父账单按自然日拆分）
 * GET    /crm/bill/{id}          账单详情（账单 + 差异 + 用量条数）
 * GET    /crm/bill/{id}/usages   账单归属用量明细分页
 * POST   /crm/bill/generate      生成账单（渠道 + 账期）
 * POST   /crm/bill/audit         复核（确认 / 差异挂起）
 * POST   /crm/bill/settle/{id}   结算（资金扣减 / 线下收款）
 * POST   /crm/bill/settle/cancel/{id} 结算撤销（红冲资金流水）
 * POST   /crm/bill/export        导出账单汇总
 * GET    /crm/bill/diff/list     差异清单
 * POST   /crm/bill/diff/handle   差异处理（调账 / 豁免 / 驳回）
 * POST   /crm/bill/diff/export   导出差异清单
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/bill")
public class CrmContractBillController extends BaseController
{
    @Autowired
    private ICrmContractBillService crmContractBillService;

    /**
     * 查询账单列表
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmContractBillDto query)
    {
        startPage();
        List<CrmContractBillDto> list = crmContractBillService.selectCrmContractBillList(query);
        return getDataTable(list);
    }

    /**
     * 查询子账单分页列表
     *
     * <p>子账单是父账单按自然日的拆分结果，供「子账单明细」二级页面展示；
     * 指定 billId 时先做数据权限校验，避免通过 billId 探测其他渠道的账单。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:list')")
    @GetMapping("/sub/list")
    public TableDataInfo subList(CrmContractBillSubDto query)
    {
        if (query.getBillId() != null)
        {
            crmContractBillService.assertBillAccessible(query.getBillId());
        }
        startPage();
        List<CrmContractBillSubDto> list = crmContractBillService.selectCrmContractBillSubList(query);
        return getDataTable(list);
    }

    /**
     * 查询账单详情
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmContractBillService.selectCrmContractBillDetail(id));
    }

    /**
     * 分页查询账单归属的用量明细
     *
     * <p>账单按「合同 + 渠道 + 账期」归集，单张账单可能挂整月流水，故必须分页；
     * 数据权限校验放在 {@code startPage()} 之前，避免分页插件作用到校验语句上。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:list')")
    @GetMapping("/{id}/usages")
    public TableDataInfo usages(@PathVariable("id") Long id)
    {
        crmContractBillService.assertBillAccessible(id);
        startPage();
        return getDataTable(crmContractBillService.selectBillUsagePage(id));
    }

    /**
     * 生成账单
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:generate')")
    @RepeatSubmit
    @Log(title = "对账账单", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody CrmBillGenerateVo vo)
    {
        return success(crmContractBillService.generate(vo));
    }

    /**
     * 账单复核
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:audit')")
    @RepeatSubmit
    @Log(title = "对账账单", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody CrmBillAuditVo vo)
    {
        return toAjax(crmContractBillService.audit(vo));
    }

    /**
     * 账单结算
     *
     * <p>请求体可选：不传时按「资金扣减 + 应付全额」处理，兼容未升级的前端。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:settle')")
    @RepeatSubmit
    @Log(title = "对账账单", businessType = BusinessType.UPDATE)
    @PostMapping("/settle/{id}")
    public AjaxResult settle(@PathVariable("id") Long id,
                             @RequestBody(required = false) CrmBillSettleVo vo)
    {
        return success(crmContractBillService.settle(id, vo));
    }

    /**
     * 结算预览：应付金额 + 客户可用余额
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:settle')")
    @GetMapping("/settle/preview/{id}")
    public AjaxResult settlePreview(@PathVariable("id") Long id)
    {
        return success(crmContractBillService.previewSettle(id));
    }

    /**
     * 结算撤销（红冲资金流水并回退余额）
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:settle-cancel')")
    @RepeatSubmit
    @Log(title = "对账账单", businessType = BusinessType.UPDATE)
    @PostMapping("/settle/cancel/{id}")
    public AjaxResult cancelSettle(@PathVariable("id") Long id,
                                   @RequestParam(value = "remark", required = false) String remark)
    {
        crmContractBillService.cancelSettle(id, remark);
        return success();
    }

    /**
     * 导出账单汇总
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:export')")
    @Log(title = "对账账单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmContractBillDto query)
    {
        List<CrmContractBillDto> list = crmContractBillService.selectCrmContractBillList(query);
        ExcelUtil<CrmContractBillDto> util = new ExcelUtil<CrmContractBillDto>(CrmContractBillDto.class);
        util.exportExcel(response, list, "对账账单");
    }

    /**
     * 查询差异清单
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:diff')")
    @GetMapping("/diff/list")
    public TableDataInfo diffList(CrmContractBillDiffDto query)
    {
        startPage();
        List<CrmContractBillDiffDto> list = crmContractBillService.selectDiffList(query);
        return getDataTable(list);
    }

    /**
     * 差异处理
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:diff-handle')")
    @RepeatSubmit
    @Log(title = "对账差异", businessType = BusinessType.UPDATE)
    @PostMapping("/diff/handle")
    public AjaxResult handleDiff(@RequestBody CrmBillDiffHandleVo vo)
    {
        return toAjax(crmContractBillService.handleDiff(vo));
    }

    /**
     * 导出差异清单
     */
    @PreAuthorize("@ss.hasPermi('crm:bill:diff')")
    @Log(title = "对账差异", businessType = BusinessType.EXPORT)
    @PostMapping("/diff/export")
    public void exportDiff(HttpServletResponse response, CrmContractBillDiffDto query)
    {
        List<CrmContractBillDiffDto> list = crmContractBillService.selectDiffList(query);
        ExcelUtil<CrmContractBillDiffDto> util = new ExcelUtil<CrmContractBillDiffDto>(CrmContractBillDiffDto.class);
        util.exportExcel(response, list, "对账差异");
    }
}
