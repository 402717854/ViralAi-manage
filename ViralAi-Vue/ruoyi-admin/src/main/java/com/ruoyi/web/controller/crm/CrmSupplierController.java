package com.ruoyi.web.controller.crm;

import java.util.List;

import com.ruoyi.crm.dto.CrmSupplierDto;
import com.ruoyi.crm.dto.CrmSupplierOptionDto;
import com.ruoyi.crm.dto.CrmSupplierVo;
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
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.service.ICrmSupplierService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * CRM 供应商Controller
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
@RestController
@RequestMapping("/crm/supplier")
public class CrmSupplierController extends BaseController
{
    @Autowired
    private ICrmSupplierService crmSupplierService;

    /**
     * 查询CRM 供应商列表
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmSupplier crmSupplier)
    {
        startPage();
        List<CrmSupplierDto> list = crmSupplierService.selectCrmSupplierList(crmSupplier);
        return getDataTable(list);
    }

    /**
     * 导出CRM 供应商列表
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:export')")
    @Log(title = "CRM 供应商", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmSupplier crmSupplier)
    {
        List<CrmSupplierDto> list = crmSupplierService.selectCrmSupplierList(crmSupplier);
        ExcelUtil<CrmSupplierDto> util = new ExcelUtil<CrmSupplierDto>(CrmSupplierDto.class);
        util.exportExcel(response, list, "CRM 供应商数据");
    }

    /**
     * 查询所有可用状态的供应商选项
     * <p>
     * 返回 id、name 两个字段，供其它模块的供应商选择器使用。仅要求登录，不校验功能权限。
     */
    @GetMapping("/options")
    public AjaxResult options()
    {
        List<CrmSupplierOptionDto> list = crmSupplierService.selectAvailableSupplierOptions();
        return success(list);
    }

    /**
     * 获取CRM 供应商详细信息
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmSupplierService.selectCrmSupplierById(id));
    }

    /**
     * 按供应商ID查询其产品关联明细
     * <p>
     * 返回该供应商关联的每个模型：projectId（模型ID）、projectNo（模型型号）、
     * projectName（模型厂商），以及成本/售价/代理三档折扣区间与预计日消耗金额。
     * 模型已被删除时仍返回关联行，projectNo / projectName 为 null。
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:query')")
    @GetMapping(value = "/{id}/projects")
    public AjaxResult getProjects(@PathVariable("id") Long id)
    {
        return success(crmSupplierService.selectSupplierProjectListBySupplierId(id));
    }

    /**
     * 新增CRM 供应商
     * <p>
     * 请求体在供应商主表字段之外支持 projects 列表，用于一次性提交供应商产品关联信息（一对多）。
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:add')")
    @Log(title = "CRM 供应商", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmSupplierVo crmSupplier)
    {
        return toAjax(crmSupplierService.insertCrmSupplier(crmSupplier));
    }

    /**
     * 修改CRM 供应商
     * <p>
     * projects 为 null 时不涉及关联变更；为空数组时清空该供应商全部产品关联；非空时全量替换。
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:edit')")
    @Log(title = "CRM 供应商", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmSupplierVo crmSupplier)
    {
        return toAjax(crmSupplierService.updateCrmSupplier(crmSupplier));
    }

    /**
     * 删除CRM 供应商
     */
    @PreAuthorize("@ss.hasPermi('crm:supplier:remove')")
    @Log(title = "CRM 供应商", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmSupplierService.deleteCrmSupplierByIds(ids));
    }
}
