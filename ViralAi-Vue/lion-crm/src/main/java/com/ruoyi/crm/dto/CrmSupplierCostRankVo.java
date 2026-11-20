package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 渠道成本排行
 *
 * <p>按渠道（供应商）聚合周期内的渠道成本，倒序取前 N 条，用于回答「钱花在哪个上游最多」。
 * 与「客户 + 渠道汇总」互补：汇总看客户侧收益，本排行看上游投入。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmSupplierCostRankVo
{
    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    private String supplierName;

    /** 调用次数 */
    private Long totalCalls;

    /** 总token */
    private Long totalTokens;

    /** 应收总额 */
    private BigDecimal totalSellAmount;

    /** 成本总额 */
    private BigDecimal totalCostAmount;

    /** 毛利总额 */
    private BigDecimal totalGrossProfit;

    /** 成本占比（该渠道成本 / 周期总成本，百分比，保留 2 位） */
    private BigDecimal costRate;

    /** 毛利率（百分比，保留 2 位） */
    private BigDecimal profitRate;
}
