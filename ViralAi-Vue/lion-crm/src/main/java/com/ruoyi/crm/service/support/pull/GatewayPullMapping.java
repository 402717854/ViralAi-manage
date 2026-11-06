package com.ruoyi.crm.service.support.pull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmGatewayPullMap;

/**
 * 上游标识映射的内存索引
 *
 * <p>一次拉取会涉及成百上千条流水，逐条查库不可接受，因此拉取开始时把涉及的
 * 渠道与账户规则一次载入内存，按四种优先级依次命中：</p>
 * <ol>
 *   <li>账户级 + 变体精确</li>
 *   <li>渠道级 + 变体精确</li>
 *   <li>账户级 + 忽略变体</li>
 *   <li>渠道级 + 忽略变体</li>
 * </ol>
 *
 * <p>客户映射没有「变体」概念，第 3、4 级退化为「账户默认客户」「渠道默认客户」，
 * 用于 pixverse 这类上游记录里根本没有客户字段的情况。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
public class GatewayPullMapping
{
    /** 空映射：未配置任何规则时使用，客户一律解析不出，模型退回上游标识 */
    private static final GatewayPullMapping EMPTY = new GatewayPullMapping();

    /** 客户映射：key 见 {@link #key(Long, Long, String, String)} */
    private final Map<String, Long> customerIndex = new HashMap<>();

    /** 模型映射：上游模型标识 → product_code */
    private final Map<String, String> productIndex = new HashMap<>();

    /**
     * 用规则清单构建索引
     *
     * @param maps 规则清单
     * @return 索引
     */
    public static GatewayPullMapping of(List<CrmGatewayPullMap> maps)
    {
        GatewayPullMapping mapping = new GatewayPullMapping();
        if (maps == null || maps.isEmpty())
        {
            return mapping;
        }
        for (CrmGatewayPullMap map : maps)
        {
            if (map == null || map.getSupplierId() == null)
            {
                continue;
            }
            Long accountId = map.getSupplierAccountId() == null ? 0L : map.getSupplierAccountId();
            String k = key(map.getSupplierId(), accountId, map.getUpstreamKey(), map.getUpstreamVariant());
            if (CrmGatewayPullMap.TYPE_CUSTOMER.equalsIgnoreCase(map.getMapType()))
            {
                if (map.getTargetId() != null)
                {
                    mapping.customerIndex.put(k, map.getTargetId());
                }
            }
            else if (CrmGatewayPullMap.TYPE_PRODUCT.equalsIgnoreCase(map.getMapType()))
            {
                if (StringUtils.isNotEmpty(map.getTargetCode()))
                {
                    mapping.productIndex.put(k, map.getTargetCode());
                }
            }
        }
        return mapping;
    }

    /**
     * 空索引
     *
     * @return 不含任何规则的索引
     */
    public static GatewayPullMapping empty()
    {
        return EMPTY;
    }

    /**
     * 解析上游用户对应的客户
     *
     * @param supplierId 渠道ID
     * @param accountId 账户ID
     * @param upstreamUser 上游用户名，可为空（空表示该账户默认客户）
     * @return 客户ID，无法解析返回 null
     */
    public Long resolveCustomerId(Long supplierId, Long accountId, String upstreamUser)
    {
        String user = StringUtils.isEmpty(upstreamUser) ? "" : upstreamUser.trim();
        Long aid = accountId == null ? 0L : accountId;
        // 1. 账户级精确；2. 渠道级精确
        Long customerId = customerIndex.get(key(supplierId, aid, user, ""));
        if (customerId == null)
        {
            customerId = customerIndex.get(key(supplierId, 0L, user, ""));
        }
        if (customerId != null || StringUtils.isEmpty(user))
        {
            return customerId;
        }
        // 3. 账户默认客户；4. 渠道默认客户
        customerId = customerIndex.get(key(supplierId, aid, "", ""));
        if (customerId == null)
        {
            customerId = customerIndex.get(key(supplierId, 0L, "", ""));
        }
        return customerId;
    }

    /**
     * 解析上游模型标识对应的模型编码
     *
     * @param supplierId 渠道ID
     * @param accountId 账户ID
     * @param modelKey 上游模型标识
     * @param variant 上游变体（如 720p|text_to_video），可为空
     * @return product_code，未配置映射返回 null（调用方自行退化）
     */
    public String resolveProductCode(Long supplierId, Long accountId, String modelKey, String variant)
    {
        if (StringUtils.isEmpty(modelKey))
        {
            return null;
        }
        String key = modelKey.trim();
        String var = StringUtils.isEmpty(variant) ? "" : variant.trim();
        Long aid = accountId == null ? 0L : accountId;

        String code = productIndex.get(key(supplierId, aid, key, var));
        if (code == null)
        {
            code = productIndex.get(key(supplierId, 0L, key, var));
        }
        if (code != null)
        {
            return code;
        }
        code = productIndex.get(key(supplierId, aid, key, ""));
        if (code == null)
        {
            code = productIndex.get(key(supplierId, 0L, key, ""));
        }
        return code;
    }

    /**
     * 索引键：渠道 + 账户 + 上游标识 + 变体
     *
     * @param supplierId 渠道ID
     * @param accountId 账户ID
     * @param upstreamKey 上游标识
     * @param variant 变体
     * @return 键
     */
    private static String key(Long supplierId, Long accountId, String upstreamKey, String variant)
    {
        return (supplierId == null ? 0L : supplierId) + ":" + (accountId == null ? 0L : accountId)
                + ":" + (upstreamKey == null ? "" : upstreamKey)
                + ":" + (variant == null ? "" : variant);
    }
}
