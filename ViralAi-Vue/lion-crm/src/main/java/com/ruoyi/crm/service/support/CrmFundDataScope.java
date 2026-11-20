package com.ruoyi.crm.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.mapper.CrmCustomerMapper;

/**
 * 客户资金数据权限
 *
 * <p>与 {@link CrmFinanceDataScope} 的渠道维度不同，资金按<b>客户负责人</b>过滤：</p>
 * <ol>
 *   <li>管理员 / 拥有 {@code crm:usage:all} 或 {@code crm:fund:all}：不限客户；</li>
 *   <li>其他账号：仅可见 {@code crm_customer.owner_user = 当前用户ID} 的客户；</li>
 *   <li>无登录上下文（定时任务）：不施加过滤。</li>
 * </ol>
 *
 * <p>为什么不复用 CrmFinanceDataScope：后者按渠道归属判断，
 * 套到客户资金上会出现「客户在 A 渠道有账单 → 就认为该财务有权看这个客户的钱」的错误结论。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Component
public class CrmFundDataScope
{
    /** 资金全量查看权限 */
    public static final String PERM_ALL = "crm:fund:all";

    /** 财务对账全量查看权限（已持有该权限的账号同样视为全量） */
    private static final String PERM_FINANCE_ALL = "crm:usage:all";

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    /**
     * 当前是否存在登录上下文
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
     * 是否不限客户
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
            return SecurityUtils.hasPermi(PERM_ALL) || SecurityUtils.hasPermi(PERM_FINANCE_ALL);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * 数据权限过滤值：为空表示不限，否则为客户负责人账号ID
     *
     * @return owner_user 值
     */
    public String scopeOwnerUser()
    {
        if (hasAllScope())
        {
            return null;
        }
        return String.valueOf(SecurityUtils.getUserId());
    }

    /**
     * 为列表查询追加数据权限条件（写入 params.ownerUser）
     *
     * @param query 查询对象
     */
    public void applyScope(BaseEntity query)
    {
        String owner = scopeOwnerUser();
        if (owner == null)
        {
            return;
        }
        query.getParams().put("ownerUser", owner);
    }

    /**
     * 校验客户是否在数据权限范围内
     *
     * @param customerId 客户ID
     */
    public void assertCustomerAccessible(Long customerId)
    {
        String owner = scopeOwnerUser();
        if (owner == null || customerId == null)
        {
            return;
        }
        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(customerId);
        if (customer == null)
        {
            throw new ServiceException("客户不存在");
        }
        if (!owner.equals(customer.getOwnerUser()))
        {
            throw new ServiceException("无权操作该客户的资金");
        }
    }
}
