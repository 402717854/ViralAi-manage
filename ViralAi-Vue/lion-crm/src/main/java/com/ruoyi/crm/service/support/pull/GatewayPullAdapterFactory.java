package com.ruoyi.crm.service.support.pull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 拉取适配器工厂
 *
 * <p>按 {@code crm_supplier_account.pull_channel} 选择实现；未配置时回退到通用适配器
 * （旧的全局 pull-url 方式）。配置了不认识的标识直接抛错，不做静默降级——
 * 静默降级会让人以为拉到了数据，实际一条都没有。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Component
public class GatewayPullAdapterFactory
{
    /** 已注册的适配器：标识 → 实现 */
    private final Map<String, GatewayPullAdapter> adapters = new LinkedHashMap<>();

    public GatewayPullAdapterFactory(List<GatewayPullAdapter> adapterList)
    {
        if (adapterList != null)
        {
            for (GatewayPullAdapter adapter : adapterList)
            {
                if (adapter == null || StringUtils.isEmpty(adapter.channel()))
                {
                    continue;
                }
                String key = adapter.channel().trim().toLowerCase();
                adapters.put(key, adapter);
            }
        }
    }

    /**
     * 取适配器
     *
     * @param channel 适配器标识，可为空
     * @return 适配器
     */
    public GatewayPullAdapter get(String channel)
    {
        if (StringUtils.isEmpty(channel))
        {
            throw new ServiceException("未注册通用拉取适配器，请检查 GatewayPullAdapter 实现");
        }
        GatewayPullAdapter adapter = adapters.get(channel.trim().toLowerCase());
        if (adapter == null)
        {
            throw new ServiceException("不支持的拉取适配器：" + channel
                    + "，可选值：" + String.join(" / ", adapters.keySet()));
        }
        return adapter;
    }

    /**
     * 是否支持该适配器标识
     *
     * @param channel 标识
     * @return true 支持
     */
    public boolean supports(String channel)
    {
        if (StringUtils.isEmpty(channel))
        {
            return false;
        }
        return adapters.containsKey(channel.trim().toLowerCase());
    }

    /**
     * 已注册的适配器标识
     *
     * @return 标识集合
     */
    public java.util.Set<String> channels()
    {
        return adapters.keySet();
    }

    /**
     * 解析实际请求地址：账户配置优先，其次适配器默认值
     *
     * @param account 账户配置的地址
     * @param adapter 适配器
     * @return 地址，都为空时返回空串
     */
    public String resolveUrl(String account, GatewayPullAdapter adapter)
    {
        if (StringUtils.isNotEmpty(account))
        {
            return account.trim();
        }
        return adapter == null || adapter.defaultUrl() == null ? "" : adapter.defaultUrl();
    }
}
