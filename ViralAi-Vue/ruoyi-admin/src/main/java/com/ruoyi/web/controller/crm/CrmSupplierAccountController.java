package com.ruoyi.web.controller.crm;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.service.ICrmSupplierAccountService;

/**
 * 渠道账户Controller
 * <p>
 * 提供渠道账户的新增、修改与列表查询。列表返回的 apiKey 已脱敏，
 * 详情接口返回完整 apiKey 供编辑回填；账户密码不回传，修改时留空表示不变更。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@RestController
@RequestMapping("/crm/supplier-account")
public class CrmSupplierAccountController extends BaseController
{
    @Autowired
    private ICrmSupplierAccountService crmSupplierAccountService;

    /**
     * 查询渠道账户列表
     */
    @PreAuthorize("@ss.hasPermi('crm:account:list')")
    @GetMapping("/list")
    public TableDataInfo list(CrmSupplierAccount crmSupplierAccount)
    {
        startPage();
        List<CrmSupplierAccountDto> list = crmSupplierAccountService.selectCrmSupplierAccountList(crmSupplierAccount);
        return getDataTable(list);
    }

    /**
     * 获取渠道账户详细信息（apiKey 为完整值）
     * <p>
     * 该接口同时服务于「修改」表单的密钥回填，因此新增/修改权限也允许访问，
     * 避免角色只授予 edit 而未授予 query 时修改操作直接 403。
     */
    @PreAuthorize("@ss.hasAnyPermi('crm:account:query,crm:account:edit,crm:account:add')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(crmSupplierAccountService.selectCrmSupplierAccountById(id));
    }

    /**
     * 新增渠道账户
     */
    @PreAuthorize("@ss.hasPermi('crm:account:add')")
    @Log(title = "渠道账户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CrmSupplierAccount crmSupplierAccount)
    {
        return toAjax(crmSupplierAccountService.insertCrmSupplierAccount(crmSupplierAccount));
    }

    /**
     * 修改渠道账户
     */
    @PreAuthorize("@ss.hasPermi('crm:account:edit')")
    @Log(title = "渠道账户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CrmSupplierAccount crmSupplierAccount)
    {
        return toAjax(crmSupplierAccountService.updateCrmSupplierAccount(crmSupplierAccount));
    }
}
