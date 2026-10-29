package com.ruoyi.web.controller.crm;

import java.util.List;

import com.ruoyi.crm.dto.CrmModelProductDto;
import com.ruoyi.crm.dto.CrmModelProductFormDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.service.ICrmModelProductService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * CRM模型产品Controller
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@RestController
@RequestMapping("/crm/product")
public class CrmModelProductController extends BaseController
{
    @Autowired
    private ICrmModelProductService crmModelProductService;

    /**
     * 查询CRM模型产品列表
     */
    @PreAuthorize("@ss.hasPermi('crm:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmModelProduct crmModelProduct)
    {
        startPage();
        List<CrmModelProductDto> list = crmModelProductService.selectCrmModelProductList(crmModelProduct);
        return getDataTable(list);
    }

    /**
     * 导出CRM模型产品列表
     */
    @PreAuthorize("@ss.hasPermi('crm:product:export')")
    @Log(title = "CRM模型产品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmModelProduct crmModelProduct)
    {
        List<CrmModelProductDto> list = crmModelProductService.selectCrmModelProductList(crmModelProduct);
        ExcelUtil<CrmModelProductDto> util = new ExcelUtil<CrmModelProductDto>(CrmModelProductDto.class);
        util.exportExcel(response, list, "CRM模型产品数据");
    }

    /**
     * 获取CRM模型产品详细信息（含计费配置）
     */
    @PreAuthorize("@ss.hasPermi('crm:product:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmModelProductService.selectCrmModelProductDetail(id));
    }

    /**
     * 新增CRM模型产品
     * <p>
     * 请求体中的 billing 非空时，在同一事务内联动新增模型计费配置。
     */
    @PreAuthorize("@ss.hasPermi('crm:product:add')")
    @Log(title = "CRM模型产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmModelProductFormDto form)
    {
        form.setCreator(getUserId());
        form.setUpdater(getUserId());
        return toAjax(crmModelProductService.insertCrmModelProductWithBilling(form));
    }

    /**
     * 修改CRM模型产品
     * <p>
     * 请求体中的 billing 非空时，按模型编码联动保存计费（存在则更新，不存在则新增）。
     */
    @PreAuthorize("@ss.hasPermi('crm:product:edit')")
    @Log(title = "CRM模型产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmModelProductFormDto form)
    {
        form.setUpdater(getUserId());
        return toAjax(crmModelProductService.updateCrmModelProductWithBilling(form));
    }

    /**
     * 删除CRM模型产品
     */
    @PreAuthorize("@ss.hasPermi('crm:product:remove')")
    @Log(title = "CRM模型产品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmModelProductService.deleteCrmModelProductByIds(ids));
    }

    /**
     * 查询所有可用得CRM模型产品列表
     */
    @GetMapping("/listAll")
    public AjaxResult listAll()
    {
        CrmModelProduct crmModelProduct=new CrmModelProduct();
        crmModelProduct.setDeleted(0);
        crmModelProduct.setStatus(1);
        List<CrmModelProductDto> list = crmModelProductService.selectCrmModelProductList(crmModelProduct);
        return success(list);
    }
}
