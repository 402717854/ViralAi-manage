package com.ruoyi.crm.service.support;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.mapper.CrmBusinessMapper;

/**
 * 商机数据权限
 *
 * <p>商机不再直接绑定渠道，可见范围改为按「客户归属优先、创建人次之」收敛：</p>
 * <ol>
 *   <li>超级管理员或拥有 {@code crm:business:all} 权限：全量可见（运营 / 管理人员）；</li>
 *   <li>账号是某客户负责人（crm_customer.owner_user = 登录账号）：该客户下全部商机；</li>
 *   <li>其他账号：仅本人创建的商机（销售）。</li>
 * </ol>
 *
 * <p>写操作（新增/编辑/状态流转）仍由接口权限 {@code crm:business:add/edit} 控制。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component
public class CrmBusinessDataScope
{
    /** 全量查看权限标识 */
    public static final String PERM_ALL = "crm:business:all";

    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

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
            // 非 Web 上下文无登录信息，按无全量权限处理
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
        return crmBusinessMapper.selectOwnedCustomerIds(loginName);
    }

    /**
     * 为列表查询追加数据权限过滤条件
     *
     * @param query 查询条件
     */
    public void applyListScope(CrmBusinessDto query)
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
     * 校验商机是否在数据权限范围内，越权直接抛业务异常
     *
     * @param business 商机
     */
    public void assertViewable(CrmBusiness business)
    {
        if (business == null)
        {
            throw new ServiceException("商机不存在或已删除");
        }
        if (hasAllScope())
        {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        if (userId != null && userId.equals(business.getCreator()))
        {
            return;
        }
        List<Long> customerIds = ownedCustomerIds();
        if (customerIds != null && business.getCustomerId() != null
                && customerIds.contains(business.getCustomerId()))
        {
            return;
        }
        throw new ServiceException("无权访问该商机");
    }
}
