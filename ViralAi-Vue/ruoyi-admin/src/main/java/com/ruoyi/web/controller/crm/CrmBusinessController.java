package com.ruoyi.web.controller.crm;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessSettleVo;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;
import com.ruoyi.crm.service.ICrmBusinessService;

/**
 * CRM 商机Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/business/list              分页列表（名称/客户/渠道/负责人/状态/成交时间/合同状态/结算状态）
 * GET    /crm/business/{id}              详情（主信息 + 意向产品 + 关联合同 + 操作日志 + 可执行动作）
 * POST   /crm/business/save              新建 / 编辑（仅初始化、跟进中）
 * POST   /crm/business/update-status     状态流转（严格按状态机白名单）
 * POST   /crm/business/update-settle     结算状态回填（仅成交）
 * POST   /crm/business/convert-contract  成交商机一键转合同，返回合同ID
 * POST   /crm/business/export            导出
 * DELETE /crm/business/{ids}             删除（仅初始化、跟进中）
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/business")
public class CrmBusinessController extends BaseController
{
    @Autowired
    private ICrmBusinessService crmBusinessService;

    /**
     * 查询商机列表
     */
    @PreAuthorize("@ss.hasPermi('crm:business:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmBusinessDto query)
    {
        startPage();
        List<CrmBusinessDto> list = crmBusinessService.selectCrmBusinessList(query);
        return getDataTable(list);
    }

    /**
     * 导出商机列表
     */
    @PreAuthorize("@ss.hasPermi('crm:business:export')")
    @Log(title = "CRM 商机", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CrmBusinessDto query)
    {
        List<CrmBusinessDto> list = crmBusinessService.selectCrmBusinessList(query);
        ExcelUtil<CrmBusinessDto> util = new ExcelUtil<CrmBusinessDto>(CrmBusinessDto.class);
        util.exportExcel(response, list, "商机数据");
    }

    /**
     * 获取商机详情
     */
    @PreAuthorize("@ss.hasPermi('crm:business:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmBusinessService.selectCrmBusinessById(id));
    }

    /**
     * 新建或编辑商机
     *
     * <p>id 为空表示新建（状态强制初始化）；id 非空表示编辑，仅初始化/跟进中允许，渠道不可变更。</p>
     */
    @PreAuthorize("@ss.hasAnyPermi('crm:business:add,crm:business:edit')")
    @Log(title = "CRM 商机", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/save")
    public AjaxResult save(@RequestBody CrmBusinessVo vo)
    {
        Long id = crmBusinessService.saveCrmBusiness(vo);
        return success(id);
    }

    /**
     * 商机状态流转
     *
     * <p>按 {@link com.ruoyi.crm.enums.BusStatusEnum} 白名单校验；流转到中断必须填写备注，
     * 流转到成交自动补齐成交时间并校验意向清单非空。</p>
     */
    @PreAuthorize("@ss.hasPermi('crm:business:edit')")
    @Log(title = "CRM 商机状态流转", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/update-status")
    public AjaxResult updateStatus(@Valid @RequestBody CrmBusinessStatusVo statusVo)
    {
        return toAjax(crmBusinessService.changeBusStatus(statusVo));
    }

    /**
     * 回填结算状态（运营 / 管理人员）
     */
    @PreAuthorize("@ss.hasPermi('crm:business:settle')")
    @Log(title = "CRM 商机结算回填", businessType = BusinessType.UPDATE)
    @PostMapping("/update-settle")
    public AjaxResult updateSettle(@Valid @RequestBody CrmBusinessSettleVo settleVo)
    {
        return toAjax(crmBusinessService.updateSettleStatus(settleVo));
    }

    /**
     * 成交商机一键转合同：复制意向清单生成草稿合同，返回合同ID供前端跳转合同编辑页
     */
    @PreAuthorize("@ss.hasPermi('crm:business:convert')")
    @Log(title = "CRM 商机转合同", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/convert-contract")
    public AjaxResult convertContract(@RequestBody CrmBusinessVo vo)
    {
        if (vo == null || vo.getId() == null)
        {
            return error("商机ID不能为空");
        }
        return success(crmBusinessService.convertContract(vo.getId()));
    }

    /**
     * 删除商机（仅初始化、跟进中，且未生成合同）
     */
    @PreAuthorize("@ss.hasPermi('crm:business:remove')")
    @Log(title = "CRM 商机", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmBusinessService.deleteCrmBusinessByIds(ids));
    }
}
