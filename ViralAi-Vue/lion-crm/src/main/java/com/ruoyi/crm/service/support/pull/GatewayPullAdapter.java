package com.ruoyi.crm.service.support.pull;

import java.util.List;

/**
 * 网关流水拉取适配器
 *
 * <p>每个上游一个实现，负责协议差异：鉴权方式、分页方式、时间格式、字段命名。
 * 实现类只管把上游记录摊平成 {@link GatewayPullRow}，不接触客户/模型映射，
 * 也不写库——这两件事由拉取服务统一处理，保证不同上游的入库口径一致。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
public interface GatewayPullAdapter
{
    /**
     * 适配器标识，与 {@code crm_supplier_account.pull_channel} 对应
     *
     * @return 标识
     */
    public String channel();

    /**
     * 未配置 pull_url 时使用的默认接口地址
     *
     * @return 默认地址，返回空串表示必须显式配置
     */
    public String defaultUrl();

    /**
     * 从上游拉取指定区间的流水
     *
     * @param context 拉取上下文
     * @return 流水行（已摊平，未做客户/模型映射）
     */
    public List<GatewayPullRow> pull(GatewayPullContext context);
}
