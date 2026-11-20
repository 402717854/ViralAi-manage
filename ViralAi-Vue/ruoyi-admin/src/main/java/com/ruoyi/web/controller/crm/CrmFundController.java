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
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.dto.CrmCustomerAccountDto;
import com.ruoyi.crm.dto.CrmCustomerAccountFlowDto;
import com.ruoyi.crm.dto.CrmCustomerRechargeDto;
import com.ruoyi.crm.dto.CrmCustomerRefundDto;
import com.ruoyi.crm.dto.CrmFundAdjustVo;
import com.ruoyi.crm.dto.CrmFundAuditVo;
import com.ruoyi.crm.dto.CrmFundInitVo;
import com.ruoyi.crm.dto.CrmFundRechargeVo;
import com.ruoyi.crm.dto.CrmFundRefundVo;
import com.ruoyi.crm.service.ICrmFundService;

/**
 * 客户资金Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/fund/account/list        客户资金列表（客户维度，含 0 余额客户）
 * GET    /crm/fund/account/{customerId} 账户详情
 * GET    /crm/fund/summary             顶部汇总（余额合计 / 本月充值扣减退款 / 待审核数）
 * GET    /crm/fund/flow/list           资金流水分页
 * GET    /crm/fund/recharge/list       充值单分页
 * POST   /crm/fund/recharge            充值登记
 * POST   /crm/fund/recharge/audit      充值审核（通过即入账）
 * GET    /crm/fund/refund/list         退款单分页
 * POST   /crm/fund/refund              退款申请
 * POST   /crm/fund/refund/audit        退款审批（通过即扣余额）
 * POST   /crm/fund/adjust              人工调账
 * POST   /crm/fund/init                期初余额批量导入
 * GET    /crm/fund/check               账实一致性校验
 * POST   /crm/fund/export              导出账户列表
 * POST   /crm/fund/flow/export         导出流水
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@RestController
@RequestMapping("/crm/fund")
public class CrmFundController extends BaseController
{
    @Autowired
    private ICrmFundService crmFundService;

    /**
     * 客户资金列表
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/account/list")
    public TableDataInfo accountList(CrmCustomerAccountDto query)
    {
        startPage();
        List<CrmCustomerAccountDto> list = crmFundService.selectAccountList(query);
        return getDataTable(list);
    }

    /**
     * 账户详情
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/account/{customerId}")
    public AjaxResult accountDetail(@PathVariable("customerId") Long customerId)
    {
        return success(crmFundService.selectAccountDetail(customerId));
    }

    /**
     * 顶部汇总
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(crmFundService.selectSummary());
    }

    /**
     * 资金流水分页
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/flow/list")
    public TableDataInfo flowList(CrmCustomerAccountFlowDto query)
    {
        startPage();
        List<CrmCustomerAccountFlowDto> list = crmFundService.selectFlowList(query);
        return getDataTable(list);
    }

    /**
     * 充值单分页
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/recharge/list")
    public TableDataInfo rechargeList(CrmCustomerRechargeDto query)
    {
        startPage();
        List<CrmCustomerRechargeDto> list = crmFundService.selectRechargeList(query);
        return getDataTable(list);
    }

    /**
     * 充值登记
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:recharge')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.INSERT)
    @PostMapping("/recharge")
    public AjaxResult recharge(@RequestBody CrmFundRechargeVo vo)
    {
        return success(crmFundService.createRecharge(vo));
    }

    /**
     * 充值审核
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:recharge-audit')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.UPDATE)
    @PostMapping("/recharge/audit")
    public AjaxResult auditRecharge(@RequestBody CrmFundAuditVo vo)
    {
        crmFundService.auditRecharge(vo);
        return success();
    }

    /**
     * 退款单分页
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/refund/list")
    public TableDataInfo refundList(CrmCustomerRefundDto query)
    {
        startPage();
        List<CrmCustomerRefundDto> list = crmFundService.selectRefundList(query);
        return getDataTable(list);
    }

    /**
     * 退款申请
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:refund')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.INSERT)
    @PostMapping("/refund")
    public AjaxResult refund(@RequestBody CrmFundRefundVo vo)
    {
        return success(crmFundService.createRefund(vo));
    }

    /**
     * 退款审批
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:refund-audit')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.UPDATE)
    @PostMapping("/refund/audit")
    public AjaxResult auditRefund(@RequestBody CrmFundAuditVo vo)
    {
        crmFundService.auditRefund(vo);
        return success();
    }

    /**
     * 人工调账
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:adjust')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public AjaxResult adjust(@RequestBody CrmFundAdjustVo vo)
    {
        return success(crmFundService.adjust(vo));
    }

    /**
     * 期初余额批量导入
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:adjust')")
    @RepeatSubmit
    @Log(title = "客户资金", businessType = BusinessType.INSERT)
    @PostMapping("/init")
    public AjaxResult init(@RequestBody CrmFundInitVo vo)
    {
        return success(crmFundService.importInitBalance(vo));
    }

    /**
     * 账实一致性校验（应始终返回空数组）
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:list')")
    @GetMapping("/check")
    public AjaxResult check()
    {
        return success(crmFundService.checkConsistency());
    }

    /**
     * 导出账户列表
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:export')")
    @Log(title = "客户资金", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmCustomerAccountDto query)
    {
        List<CrmCustomerAccountDto> list = crmFundService.selectAccountList(query);
        ExcelUtil<CrmCustomerAccountDto> util = new ExcelUtil<CrmCustomerAccountDto>(CrmCustomerAccountDto.class);
        util.exportExcel(response, list, "客户资金");
    }

    /**
     * 导出资金流水
     */
    @PreAuthorize("@ss.hasPermi('crm:fund:export')")
    @Log(title = "客户资金", businessType = BusinessType.EXPORT)
    @PostMapping("/flow/export")
    public void exportFlow(HttpServletResponse response, CrmCustomerAccountFlowDto query)
    {
        List<CrmCustomerAccountFlowDto> list = crmFundService.selectFlowList(query);
        ExcelUtil<CrmCustomerAccountFlowDto> util = new ExcelUtil<CrmCustomerAccountFlowDto>(CrmCustomerAccountFlowDto.class);
        util.exportExcel(response, list, "客户资金流水");
    }
}
