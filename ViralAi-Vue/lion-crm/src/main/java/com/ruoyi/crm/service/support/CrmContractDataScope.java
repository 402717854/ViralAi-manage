package com.ruoyi.crm.service.support;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.dto.CrmContractDto;
import com.ruoyi.crm.mapper.CrmContractMapper;

/**
 * 合同数据权限
 *
 * <p>合同不再绑定单一渠道，可见范围改为按「客户归属优先、创建人次之」收敛：</p>
 * <ol>
 *   <li>超级管理员或拥有 {@code crm:contract:all} 权限：全量可见（运营 / 审批人）；</li>
 *   <li>账号是某客户负责人（crm_customer.owner_user = 登录账号）：该客户全部合同；</li>
 *   <li>其他账号：仅本人创建的合同（销售 / 客户账号）。</li>
 * </ol>
 *
 * <p>说明：财务对账相关数据（流水/台账/账单）仍按渠道维度收敛，见 CrmFinanceDataScope。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component
public class CrmContractDataScope
{
    /** 全量查看权限标识 */
    public static final String PERM_ALL = "crm:contract:all";

    @Autowired
    private CrmContractMapper crmContractMapper;

    /**
     * 当前账号是否拥有全量查看权限
     *
     * @return true 全量可见
     */
    public boolean hasAllScope()
    {
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
            // 非 Web 上下文（定时任务）无登录信息，按无全量权限处理
            return false;
        }
    }

    /**
     * 查询当前账号负责的客户ID集合
     *
     * @return 客户ID集合，无则返回空集合
     */
    public List<Long> ownedCustomerIds()
    {
        String loginName = SecurityUtils.getUsername();
        return crmContractMapper.selectOwnedCustomerIds(loginName);
    }

    /**
     * 为列表查询追加数据权限过滤条件
     *
     * @param query 查询条件
     */
    public void applyListScope(CrmContractDto query)
    {
        if (hasAllScope())
        {
            return;
        }
        List<Long> customerIds = ownedCustomerIds();
        if (customerIds != null && !customerIds.isEmpty())
        {
            query.getParams().put("scopeCustomerIds", customerIds);
        }
        else
        {
            query.getParams().put("scopeCreator", SecurityUtils.getUserId());
        }
    }

    /**
     * 校验合同是否在数据权限范围内，越权直接抛业务异常
     *
     * @param contract 合同
     */
    public void assertViewable(CrmContract contract)
    {
        if (contract == null)
        {
            throw new ServiceException("合同不存在或已删除");
        }
        if (hasAllScope())
        {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        if (userId != null && userId.equals(contract.getCreator()))
        {
            return;
        }
        List<Long> customerIds = ownedCustomerIds();
        if (customerIds != null && contract.getConsumerId() != null
                && customerIds.contains(contract.getConsumerId()))
        {
            return;
        }
        throw new ServiceException("无权访问该合同");
    }
}
