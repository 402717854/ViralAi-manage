package com.ruoyi.crm.service.support.pull;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * 上游流水行（适配器的统一输出）
 *
 * <p>上游接口形态各异，但落到本系统只需要有限几个字段，适配器负责把它们摊平到这里，
 * 由拉取服务统一做「客户解析 → 模型编码解析 → 组装流水」。</p>
 *
 * <p>{@code callNo} 是上游自己的唯一标识，不含渠道前缀；加前缀是为了避免不同上游
 * 的 ID 撞车（{@code gateway_call_log.uk_call_no} 是全局唯一）。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Getter
@Setter
public class GatewayPullRow
{
    /** 上游流水唯一标识（不含前缀） */
    private String upstreamId;

    /** 上游用户名：中转站用于定位客户；pixverse 无此维度，为空 */
    private String upstreamUser;

    /** 上游模型标识 */
    private String modelKey;

    /** 上游模型变体，如 720p|text_to_video；无变体为空串 */
    private String modelVariant;

    /** 调用时间 */
    private Date callTime;

    /** 输入 token */
    private long promptTokens;

    /** 输出 token */
    private long completionTokens;

    /** 总 token（上游未给时由输入+输出补齐） */
    private long totalTokens;
    /**消耗费用**/
    private BigDecimal totalFees;

    /**实际扣费**/
    private BigDecimal actualFees;

    /** 调用状态 0失败 1成功 */
    private int callStatus = 1;

    /** 失败原因 */
    private String errMsg;

    /** 上游原始计费信息（已截断到字段长度内） */
    private String rawPriceDetail;

    /** 模型类型 LLM/Image/Video 等，仅作展示 */
    private String modelType;
}
