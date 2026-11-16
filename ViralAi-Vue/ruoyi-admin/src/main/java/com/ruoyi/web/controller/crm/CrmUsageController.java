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
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierAccountVo;
import com.ruoyi.crm.dto.CrmSupplierStatVo;
import com.ruoyi.crm.dto.CrmUsageBoardVo;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;
import com.ruoyi.crm.service.ICrmContractService;
import com.ruoyi.crm.service.ICrmContractUsageService;

/**
 * 用量对账Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/usage/list              台账分页
 * GET    /crm/usage/{id}              台账详情
 * GET    /crm/usage/customer-supplier-accounts  客户可用渠道账户（上传联动）
 * POST   /crm/usage/reconcile         手动对账（区间 + 渠道）
 * POST   /crm/usage/rematch/{id}      单条重新匹配
 * GET    /crm/usage/board             渠道对账看板
 * GET    /crm/usage/supplier-stat     渠道对账汇总
 * POST   /crm/usage/export            导出台账
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/usage")
public class CrmUsageController extends BaseController
{
    @Autowired
    private ICrmContractUsageService crmContractUsageService;

    @Autowired
    private ICrmContractService crmContractService;

    /**
     * 查询台账列表
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmContractUsageDto query)
    {
        startPage();
        List<CrmContractUsageDto> list = crmContractUsageService.selectCrmContractUsageList(query);
        return getDataTable(list);
    }

    /**
     * 查询台账详情
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmContractUsageService.selectCrmContractUsageById(id));
    }

    /**
     * 客户可用的「渠道 + 渠道账户」组合
     *
     * <p>用于流水上传弹窗的三级联动：客户 → 渠道 → 渠道账户。数据取自该客户生效合同
     * （已签约 / 已到期 / 已归档）的明细行，因此下拉里只会出现合同上真实配置过的组合。
     * 属于只读字典接口，与渠道/账户的 options 接口一致，仅要求登录。</p>
     *
     * @param customerId 客户ID
     * @return 组合集合，前端按 supplierId 聚合出渠道、按 supplierAccountId 聚合出账户
     */
    @GetMapping("/customer-supplier-accounts")
    public AjaxResult customerSupplierAccounts(@RequestParam("customerId") Long customerId)
    {
        List<CrmCustomerSupplierAccountVo> list = crmContractService.selectCustomerSupplierAccounts(customerId);
        return success(list);
    }

    /**
     * 手动执行对账
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:reconcile')")
    @RepeatSubmit
    @Log(title = "用量对账", businessType = BusinessType.UPDATE)
    @PostMapping("/reconcile")
    public AjaxResult reconcile(@RequestBody CrmUsageReconcileVo vo)
    {
        return success(crmContractUsageService.reconcile(vo));
    }

    /**
     * 单条重新匹配
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:reconcile')")
    @RepeatSubmit
    @Log(title = "用量对账", businessType = BusinessType.UPDATE)
    @PostMapping("/rematch/{id}")
    public AjaxResult rematch(@PathVariable("id") Long id)
    {
        int matched = crmContractUsageService.rematch(id);
        return matched > 0 ? success("已匹配到合同并完成计费") : error("仍未匹配到生效合同，请检查合同与模型清单");
    }

    /**
     * 渠道对账看板
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:stat')")
    @GetMapping("/board")
    public AjaxResult board(@RequestParam(value = "billCycle", required = false) String billCycle)
    {
        CrmUsageBoardVo board = crmContractUsageService.board(billCycle);
        return success(board);
    }

    /**
     * 渠道对账汇总（看板副表 / 报表复用）
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:stat')")
    @GetMapping("/supplier-stat")
    public AjaxResult supplierStat(@RequestParam(value = "billCycle", required = false) String billCycle)
    {
        List<CrmSupplierStatVo> list = crmContractUsageService.selectSupplierStat(billCycle);
        return success(list);
    }

    /**
     * 导出对账台账
     */
    @PreAuthorize("@ss.hasPermi('crm:usage:export')")
    @Log(title = "用量对账", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmContractUsageDto query)
    {
        List<CrmContractUsageDto> list = crmContractUsageService.selectCrmContractUsageList(query);
        ExcelUtil<CrmContractUsageDto> util = new ExcelUtil<CrmContractUsageDto>(CrmContractUsageDto.class);
        util.exportExcel(response, list, "对账台账");
    }
}
