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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.dto.CrmContractAuditVo;
import com.ruoyi.crm.dto.CrmContractCancelVo;
import com.ruoyi.crm.dto.CrmContractDto;
import com.ruoyi.crm.dto.CrmContractInitVo;
import com.ruoyi.crm.dto.CrmContractOperateVo;
import com.ruoyi.crm.dto.CrmContractVo;
import com.ruoyi.crm.service.ICrmContractService;

/**
 * 合同管理Controller
 *
 * <p>接口清单：</p>
 * <pre>
 * GET    /crm/contract/list           分页列表
 * GET    /crm/contract/{id}           详情（主信息 + 明细 + 审批记录 + 操作日志）
 * POST   /crm/contract/init           新建初始化（按商机带出客户、渠道）
 * POST   /crm/contract/save           保存草稿（新增 / 草稿更新）
 * POST   /crm/contract/submit-audit   提交审批
 * POST   /crm/contract/audit          审批处理
 * POST   /crm/contract/cancel         合同作废
 * POST   /crm/contract/archive        合同归档
 * DELETE /crm/contract/{ids}          删除草稿
 * </pre>
 *
 * <p>提交、审批、作废、归档均加 @RepeatSubmit 防重复点击；数据权限与状态机校验在 Service 层统一处理。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/contract")
public class CrmContractController extends BaseController
{
    @Autowired
    private ICrmContractService crmContractService;

    /**
     * 查询合同分页列表
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmContractDto query)
    {
        startPage();
        List<CrmContractDto> list = crmContractService.selectCrmContractList(query);
        return getDataTable(list);
    }

    /**
     * 查询合同详情
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmContractService.selectCrmContractById(id));
    }

    /**
     * 新建合同初始化：按商机带出客户、渠道信息，此时不生成合同编号
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:add')")
    @PostMapping("/init")
    public AjaxResult init(@RequestBody CrmContractVo vo)
    {
        if (vo == null || vo.getBusId() == null)
        {
            return error("商机ID不能为空");
        }
        CrmContractInitVo initVo = crmContractService.initByBusId(vo.getBusId());
        return success(initVo);
    }

    /**
     * 保存合同（id 为空新增并生成编号，id 非空为草稿更新）
     */
    @PreAuthorize("@ss.hasAnyPermi('crm:contract:add,crm:contract:edit')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody CrmContractVo vo)
    {
        return success(crmContractService.saveCrmContract(vo));
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:submit')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PostMapping("/submit-audit")
    public AjaxResult submitAudit(@RequestBody CrmContractOperateVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            return error("合同ID不能为空");
        }
        return toAjax(crmContractService.submitAudit(vo.getContractId()));
    }

    /**
     * 撤回审批（审批中 → 草稿，仅创建人可撤回）
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:submit')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PostMapping("/withdraw")
    public AjaxResult withdraw(@RequestBody CrmContractOperateVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            return error("合同ID不能为空");
        }
        return toAjax(crmContractService.withdrawAudit(vo.getContractId()));
    }

    /**
     * 审批处理（通过 / 驳回）
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:audit')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody CrmContractAuditVo vo)
    {
        return toAjax(crmContractService.audit(vo));
    }

    /**
     * 合同作废
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:cancel')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PostMapping("/cancel")
    public AjaxResult cancel(@RequestBody CrmContractCancelVo vo)
    {
        return toAjax(crmContractService.cancel(vo));
    }

    /**
     * 合同归档
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:archive')")
    @RepeatSubmit
    @Log(title = "合同管理", businessType = BusinessType.UPDATE)
    @PostMapping("/archive")
    public AjaxResult archive(@RequestBody CrmContractOperateVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            return error("合同ID不能为空");
        }
        return toAjax(crmContractService.archive(vo.getContractId()));
    }

    /**
     * 删除草稿合同
     */
    @PreAuthorize("@ss.hasPermi('crm:contract:remove')")
    @Log(title = "合同管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(crmContractService.deleteCrmContractByIds(ids));
    }
}
