package com.ruoyi.crm.service.support.pull;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.crm.domain.CrmGatewayPullMap;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.mapper.CrmGatewayPullMapMapper;

/**
 * 上游标识映射加载器
 *
 * <p>把「按渠道/账户取规则并建索引」这件事单独抽出来，拉取服务只关心
 * {@link GatewayPullMapping#resolveCustomerId} 与
 * {@link GatewayPullMapping#resolveProductCode} 两个方法。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Component
public class GatewayPullMappingLoader
{
    @Autowired
    private CrmGatewayPullMapMapper crmGatewayPullMapMapper;

    /**
     * 载入本次拉取涉及的映射
     *
     * @param accounts 本次拉取的账户清单
     * @return 映射索引
     */
    public GatewayPullMapping load(List<CrmSupplierAccount> accounts)
    {
        if (accounts == null || accounts.isEmpty())
        {
            return GatewayPullMapping.empty();
        }
        List<Long> supplierIds = new ArrayList<>();
        List<Long> accountIds = new ArrayList<>();
        for (CrmSupplierAccount account : accounts)
        {
            if (account == null)
            {
                continue;
            }
            if (account.getSupplierId() != null && !supplierIds.contains(account.getSupplierId()))
            {
                supplierIds.add(account.getSupplierId());
            }
            if (account.getId() != null && !accountIds.contains(account.getId()))
            {
                accountIds.add(account.getId());
            }
        }
        List<CrmGatewayPullMap> maps = crmGatewayPullMapMapper.selectEffectiveMaps(supplierIds, accountIds, null);
        return GatewayPullMapping.of(maps);
    }
}
