package com.ruoyi.web.controller.crm;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.crm.dto.CrmContractProductBatchVo;
import com.ruoyi.crm.service.ICrmContractProductService;

/**
 * 合同模型产品明细Controller
 *
 * <p>明细维护仅草稿状态可用；渠道归属、折扣区间、重复模型与 JSON 合法性均由后端强校验。</p>
 *
 * <pre>
 * GET    /crm/contract-product/list/{contractId}   明细列表
 * POST   /crm/contract-product/batch-save          明细批量保存（全量替换）
 * DELETE /crm/contract-product/{id}                删除单条明细
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/contract-product")
public class CrmContractProductController extends BaseController
{
    @Autowired
    private ICrmContractProductService crmContractProductService;

    /**
     * 查询合同明细列表
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:query')")
    @GetMapping("/list/{contractId}")
    public AjaxResult list(@PathVariable("contractId") Long contractId)
    {
        return success(crmContractProductService.selectContractProductList(contractId));
    }

    /**
     * 明细批量保存（全量替换）
     */
    @PreAuthorize("@ss.hasAnyPermi('crm:contract:add,crm:contract:edit')")
    @RepeatSubmit
    @Log(title = "合同模型明细", businessType = BusinessType.UPDATE)
    @PostMapping("/batch-save")
    public AjaxResult batchSave(@RequestBody CrmContractProductBatchVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            return error("合同ID不能为空");
        }
        return toAjax(crmContractProductService.batchSaveContractProduct(vo.getContractId(), vo.getList()));
    }

    /**
     * 删除单条明细
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:edit')")
    @Log(title = "合同模型明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(crmContractProductService.deleteContractProductById(id));
    }
}
