package com.ruoyi.crm.service.support.pull;

import java.util.Date;

import com.ruoyi.crm.domain.CrmSupplierAccount;

import lombok.Getter;
import lombok.Setter;

/**
 * 单次拉取的上下文
 *
 * <p>一个账户一次拉取，账户自带密钥与协议；区间由调用方给，分页参数与保护上限
 * 由配置统一注入，避免某个上游反复返回同一页导致死循环。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Getter
@Setter
public class GatewayPullContext
{
    /** 渠道账户（含 apiKey） */
    private CrmSupplierAccount account;

    /** 起始时间（含） */
    private Date start;

    /** 结束时间（含） */
    private Date end;

    /** 已解析的接口地址 */
    private String url;

    /** 每页条数 */
    private int pageSize = 100;

    /** 最大翻页数，超过即停止并告警，防止上游分页异常导致无限循环 */
    private int maxPages = 200;

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 60000;

    /**
     * 渠道ID（取自账户）
     *
     * @return 渠道ID
     */
    public Long getSupplierId()
    {
        return account == null ? null : account.getSupplierId();
    }

    /**
     * 账户ID（取自账户）
     *
     * @return 账户ID
     */
    public Long getAccountId()
    {
        return account == null ? null : account.getId();
    }

    /**
     * 上游用户名过滤
     *
     * <p>只认账户上显式配置的 {@code pullUser}，不拿 accountName 兜底：
     * 管理员令牌拉全量时不该被账户名意外过滤掉，那会表现为「拉到了 0 条」。</p>
     *
     * @return 用户名，未配置返回 null
     */
    public String resolveUser()
    {
        if (account == null || account.getPullUser() == null || account.getPullUser().trim().isEmpty())
        {
            return null;
        }
        return account.getPullUser().trim();
    }

    /**
     * 上游密钥：取自账户 apiKey
     *
     * @return 密钥，可为空
     */
    public String resolveApiKey()
    {
        return account == null ? null : account.getApiKey();
    }
}
