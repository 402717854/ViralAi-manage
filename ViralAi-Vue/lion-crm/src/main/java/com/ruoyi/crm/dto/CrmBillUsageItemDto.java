package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单归属用量明细行
 *
 * <p>仅服务于「账单详情」抽屉的用量明细分页列表：只保留页面实际展示的字段，
 * 不再重复返回合同、客户、渠道、模型等维度信息——这些在账单主体里已是同一值，
 * 逐行返回只会放大响应体积。</p>
 *
 * <p>该查询不对任何维表做 join，配合 PageHelper 分页，单次只取一页数据。</p>
 *
 * @author ruoyi
 * @date 2026-11-16
 */
@Getter
@Setter
public class CrmBillUsageItemDto
{
    /** 主键 */
    private Long id;

    /** 网关流水号；调账冲销为 ADJ-{原流水号} */
    private String callNo;

    /** 模型编码 */
    private String productCode;

    /** 消耗token（冲销为负） */
    private Long totalTokens;

    /** 本次向客户应收金额 */
    private BigDecimal sellAmount;

    /** 渠道成本金额 */
    private BigDecimal costAmount;

    /** 毛利 = 应收 - 成本 */
    private BigDecimal grossProfit;

    /** 调用时间（计费归属） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 用量类型 0网关计费 1调账冲销 */
    private Integer usageType;

    /** 匹配状态 1匹配成功 */
    private Integer matchStatus;
}
