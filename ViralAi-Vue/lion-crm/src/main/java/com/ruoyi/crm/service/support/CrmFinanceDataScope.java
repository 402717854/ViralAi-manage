package com.ruoyi.crm.service.support;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.mapper.CrmContractMapper;

/**
 * 财务对账数据权限
 *
 * <p>规则与合同模块保持一致：</p>
 * <ol>
 *   <li>超级管理员或拥有 {@code crm:usage:all} 权限：不限渠道；</li>
 *   <li>渠道负责人（crm_supplier.owner_user = 登录账号）：仅本渠道的流水、用量、账单、差异；</li>
 *   <li>其他账号且无渠道归属：查不到任何数据（财务账号需显式授予全量查看权限）；</li>
 *   <li>定时任务等无登录上下文场景：不施加渠道过滤（任务本身按全量处理）。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component
public class CrmFinanceDataScope
{
    /** 财务对账全量查看权限标识 */
    public static final String PERM_ALL = "crm:usage:all";

    /** 无渠道归属时使用的不可命中渠道ID，避免为空集合导致 SQL 条件被跳过 */
    private static final Long UNREACHABLE_SUPPLIER_ID = -1L;

    @Autowired
    private CrmContractMapper crmContractMapper;

    /**
     * 当前是否存在登录上下文（定时任务中为 false）
     *
     * @return true 有登录用户
     */
    public boolean hasLoginContext()
    {
        try
        {
            return SecurityUtils.getUserId() != null;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 是否不限渠道
     *
     * @return true 全量可见
     */
    public boolean hasAllScope()
    {
        if (!hasLoginContext())
        {
            return true;
        }
        if (SecurityUtils.isAdmin())
        {
            return true;
        }
        try
        {
            return SecurityUtils.hasPermi(PERM_ALL);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 查询当前账号负责的渠道ID集合
     *
     * @return 渠道ID集合，无归属返回空集合
     */
    public List<Long> ownedSupplierIds()
    {
        if (!hasLoginContext())
        {
            return Collections.emptyList();
        }
        List<Long> ids = crmContractMapper.selectOwnedSupplierIds(SecurityUtils.getUsername());
        return ids == null ? Collections.emptyList() : ids;
    }

    /**
     * 解析统计类查询的渠道范围
     *
     * @return null 表示不限渠道；非 null 为允许的渠道集合
     */
    public List<Long> scopeSupplierIds()
    {
        if (hasAllScope())
        {
            return null;
        }
        List<Long> ids = ownedSupplierIds();
        if (ids.isEmpty())
        {
            return Collections.singletonList(UNREACHABLE_SUPPLIER_ID);
        }
        return ids;
    }

    /**
     * 为列表查询追加数据权限过滤条件
     *
     * @param query 查询对象（需继承 BaseEntity）
     */
    public void applyScope(BaseEntity query)
    {
        List<Long> ids = scopeSupplierIds();
        if (ids == null)
        {
            return;
        }
        query.getParams().put("scopeSupplierIds", ids);
    }

    /**
     * 校验指定渠道是否在数据权限范围内
     *
     * @param supplierId 渠道ID
     */
    public void assertSupplierAccessible(Long supplierId)
    {
        List<Long> ids = scopeSupplierIds();
        if (ids == null)
        {
            return;
        }
        if (supplierId == null || !ids.contains(supplierId))
        {
            throw new ServiceException("无权操作该渠道的对账数据");
        }
    }
}
