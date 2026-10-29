package com.ruoyi.web.controller.crm;

import java.util.List;

import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
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
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.service.ICrmCustomerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * CRM 客户Controller
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
@RestController
@RequestMapping("/crm/customer")
public class CrmCustomerController extends BaseController
{
    @Autowired
    private ICrmCustomerService crmCustomerService;

    /**
     * 查询CRM 客户列表
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmCustomer crmCustomer)
    {
        startPage();
        List<CrmCustomerDto> list = crmCustomerService.selectCrmCustomerList(crmCustomer);
        return getDataTable(list);
    }

    /**
     * 导出CRM 客户列表
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:export')")
    @Log(title = "CRM 客户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmCustomer crmCustomer)
    {
        List<CrmCustomerDto> list = crmCustomerService.selectCrmCustomerList(crmCustomer);
        ExcelUtil<CrmCustomerDto> util = new ExcelUtil<CrmCustomerDto>(CrmCustomerDto.class);
        util.exportExcel(response, list, "CRM 客户数据");
    }

    /**
     * 查询所有可用状态的客户选项
     * <p>
     * 返回 id、name 两个字段，供其它模块的客户选择器使用。仅要求登录，不校验功能权限。
     */
    @GetMapping("/options")
    public AjaxResult options()
    {
        List<CrmCustomerOptionDto> list = crmCustomerService.selectAvailableCustomerOptions();
        return success(list);
    }

    /**
     * 获取CRM 客户详细信息
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmCustomerService.selectCrmCustomerById(id));
    }

    /**
     * 新增CRM 客户
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:add')")
    @Log(title = "CRM 客户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmCustomer crmCustomer)
    {
        crmCustomer.setCreator(getUserId());
        crmCustomer.setUpdater(getUserId());
        return toAjax(crmCustomerService.insertCrmCustomer(crmCustomer));
    }

    /**
     * 修改CRM 客户
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:edit')")
    @Log(title = "CRM 客户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmCustomer crmCustomer)
    {
        crmCustomer.setUpdater(getUserId());
        return toAjax(crmCustomerService.updateCrmCustomer(crmCustomer));
    }

    /**
     * 删除CRM 客户
     */
    @PreAuthorize("@ss.hasPermi('crm:customer:remove')")
    @Log(title = "CRM 客户", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmCustomerService.deleteCrmCustomerByIds(ids));
    }
}
