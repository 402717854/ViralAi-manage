package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 手动对账请求对象
 *
 * <p>时间区间支持 {@code yyyy-MM-dd} 与 {@code yyyy-MM-dd HH:mm:ss} 两种格式；
 * 只传日期时，起始补 00:00:00、结束补 23:59:59。supplierId 为空表示全部渠道，
 * customerId 为空表示全部客户。三个条件为「与」关系，同时指定时只对账该客户在该渠道下的流水。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmUsageReconcileVo
{
    /** 起始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 渠道ID，为空表示全部渠道 */
    private Long supplierId;

    /**
     * 客户ID，为空表示全部客户
     * <p>仅收窄待对账流水的扫描范围，不改变匹配逻辑：匹配仍以流水自身的客户 + 渠道 + 模型
     * 去命中生效合同。用于补签合同后按客户单独补对账，避免整渠道全量重扫。</p>
     */
    private Long customerId;

    /**
     * 渠道账户ID，为空表示该渠道下全部启用账户
     * <p>拉取流水时生效：一个账户对应一套密钥与一种上游协议（viralai 中转站 / pixverse），
     * 对账本身不区分账户。若不指定而只指定渠道，会依次拉取该渠道下所有启用账户。</p>
     */
    private Long supplierAccountId;

    /** 是否只重试异常记录（默认 true）；false 表示同时处理待匹配记录 */
    private Boolean retryAbnormal;
}
