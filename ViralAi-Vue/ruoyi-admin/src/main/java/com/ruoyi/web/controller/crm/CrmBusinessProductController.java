package com.ruoyi.web.controller.crm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.dto.CrmBusinessProductBatchVo;
import com.ruoyi.crm.service.ICrmBusinessProductService;

/**
 * 商机意向产品Controller
 *
 * <p>产品清单与「添加渠道模型」下拉共用 /crm/product/list-by-supplier 数据源，
 * 后端对渠道归属、重复、折扣区间做二次强校验。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/business-product")
public class CrmBusinessProductController extends BaseController
{
    @Autowired
    private ICrmBusinessProductService crmBusinessProductService;

    /**
     * 查询商机意向产品清单
     */
    @PreAuthorize("@ss.hasPermi('crm:business:query')")
    @GetMapping("/list/{busId}")
    public AjaxResult list(@PathVariable("busId") Long busId)
    {
        return success(crmBusinessProductService.selectBusinessProductList(busId));
    }

    /**
     * 批量保存商机意向产品（全量替换）
     */
    @PreAuthorize("@ss.hasPermi('crm:business:product')")
    @Log(title = "商机意向产品", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/batch-save")
    public AjaxResult batchSave(@RequestBody CrmBusinessProductBatchVo vo)
    {
        if (vo == null || vo.getBusId() == null)
        {
            return error("商机ID不能为空");
        }
        return toAjax(crmBusinessProductService.batchSaveBusinessProduct(vo.getBusId(), vo.getList()));
    }

    /**
     * 删除单条商机意向产品
     * <p>
     * 需同时指定渠道：同一模型可能被多个渠道上架，行由「商机 + 渠道 + 模型」唯一确定。
     */
    @PreAuthorize("@ss.hasPermi('crm:business:product')")
    @Log(title = "商机意向产品", businessType = BusinessType.DELETE)
    @DeleteMapping
    public AjaxResult remove(@RequestParam("busId") Long busId,
                             @RequestParam("supplierId") Long supplierId,
                             @RequestParam("productId") Long productId)
    {
        return toAjax(crmBusinessProductService.deleteBusinessProduct(busId, supplierId, productId));
    }
}
