package com.ruoyi.web.controller.crm;

import java.util.List;

import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.service.ICrmBusinessService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * CRM 商机Controller
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@RestController
@RequestMapping("/crm/business")
public class CrmBusinessController extends BaseController
{
    @Autowired
    private ICrmBusinessService crmBusinessService;

    /**
     * 查询CRM 商机列表
     */
    @PreAuthorize("@ss.hasPermi('crm:business:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmBusiness crmBusiness)
    {
        startPage();
        List<CrmBusinessDto> list = crmBusinessService.selectCrmBusinessList(crmBusiness);
        return getDataTable(list);
    }

    /**
     * 导出CRM 商机列表
     */
    @PreAuthorize("@ss.hasPermi('crm:business:export')")
    @Log(title = "CRM 商机", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmBusiness crmBusiness)
    {
        List<CrmBusinessDto> list = crmBusinessService.selectCrmBusinessList(crmBusiness);
        ExcelUtil<CrmBusinessDto> util = new ExcelUtil<CrmBusinessDto>(CrmBusinessDto.class);
        util.exportExcel(response, list, "CRM 商机数据");
    }

    /**
     * 获取CRM 商机详细信息
     */
    @PreAuthorize("@ss.hasPermi('crm:business:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmBusinessService.selectCrmBusinessById(id));
    }

    /**
     * 新增CRM 商机
     */
    @PreAuthorize("@ss.hasPermi('crm:business:add')")
    @Log(title = "CRM 商机", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmBusinessVo crmBusiness)
    {
        return toAjax(crmBusinessService.insertCrmBusiness(crmBusiness));
    }

    /**
     * 修改CRM 商机
     */
    @PreAuthorize("@ss.hasPermi('crm:business:edit')")
    @Log(title = "CRM 商机", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmBusinessVo crmBusinessVo)
    {
        return toAjax(crmBusinessService.updateCrmBusiness(crmBusinessVo));
    }

    /**
     * 删除CRM 商机
     */
    @PreAuthorize("@ss.hasPermi('crm:business:remove')")
    @Log(title = "CRM 商机", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmBusinessService.deleteCrmBusinessByIds(ids));
    }

    /**
     * 商机状态流转
     *
     * <p>按 {@link com.ruoyi.crm.enums.BusStatusEnum} 定义的白名单校验流转合法性；
     * 流转到 6 中断 时必须提交中断备注，流转到 4 成交 时自动补齐成交日期（未指定则取当前时间）。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:business:edit')")
    @Log(title = "CRM 商机状态流转", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@Valid @RequestBody CrmBusinessStatusVo crmBusinessStatusVo)
    {
        return toAjax(crmBusinessService.changeBusStatus(crmBusinessStatusVo));
    }

    /**
     * 查询商机当前状态下允许流转到的目标状态（供前端渲染状态操作按钮）
     */
    @PreAuthorize("@ss.hasPermi('crm:business:query')")
    @GetMapping("/statusOption/{id}")
    public AjaxResult statusOption(@PathVariable("id") Long id)
    {
        return success(crmBusinessService.selectAllowedNextStatus(id));
    }
}
