package com.ruoyi.web.controller.crm;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.domain.CrmGatewayPullMap;
import com.ruoyi.crm.dto.CrmGatewayPullMapDto;
import com.ruoyi.crm.service.ICrmGatewayPullMapService;

/**
 * 网关流水上游标识映射Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/pull-map/list    映射列表
 * GET    /crm/pull-map/{id}    映射详情
 * POST   /crm/pull-map         新增映射
 * PUT    /crm/pull-map         修改映射
 * DELETE /crm/pull-map/{ids}   删除映射
 * POST   /crm/pull-map/export  导出映射
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@RestController
@RequestMapping("/crm/pull-map")
public class CrmGatewayPullMapController extends BaseController
{
    @Autowired
    private ICrmGatewayPullMapService crmGatewayPullMapService;

    /**
     * 查询映射列表
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmGatewayPullMap query)
    {
        startPage();
        List<CrmGatewayPullMapDto> list = crmGatewayPullMapService.selectCrmGatewayPullMapList(query);
        return getDataTable(list);
    }

    /**
     * 查询映射详情
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:list')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(crmGatewayPullMapService.selectCrmGatewayPullMapById(id));
    }

    /**
     * 新增映射
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:add')")
    @Log(title = "流水映射", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmGatewayPullMap map)
    {
        return toAjax(crmGatewayPullMapService.insertCrmGatewayPullMap(map));
    }

    /**
     * 修改映射
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:edit')")
    @Log(title = "流水映射", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmGatewayPullMap map)
    {
        return toAjax(crmGatewayPullMapService.updateCrmGatewayPullMap(map));
    }

    /**
     * 删除映射
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:remove')")
    @Log(title = "流水映射", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmGatewayPullMapService.deleteCrmGatewayPullMapByIds(ids));
    }

    /**
     * 导出映射
     */
    @PreAuthorize("@ss.hasPermi('crm:pullMap:list')")
    @Log(title = "流水映射", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmGatewayPullMap query)
    {
        List<CrmGatewayPullMapDto> list = crmGatewayPullMapService.selectCrmGatewayPullMapList(query);
        ExcelUtil<CrmGatewayPullMapDto> util = new ExcelUtil<CrmGatewayPullMapDto>(CrmGatewayPullMapDto.class);
        util.exportExcel(response, list, "流水映射");
    }
}
