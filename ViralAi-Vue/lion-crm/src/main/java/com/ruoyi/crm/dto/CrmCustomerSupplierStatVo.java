package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户 + 渠道维度对账汇总
 *
 * <p>把一个客户在同一渠道下的用量聚合为一行，用于核对「某客户在某渠道花了多少、成本多少」。
 * 相比只按渠道分组，能直接定位到具体客户的成本与毛利，是对账与调价的直接依据。</p>
 *
 * <p>口径说明：调用数、异常数包含匹配失败的用量；token、金额、毛利只统计匹配成功的用量
 * （{@code match_status = 1}），与看板概览卡片保持一致。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerSupplierStatVo
{
    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

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

    /** 毛利率（百分比，保留 2 位） */
    private BigDecimal profitRate;

    /** 参与计费的合同数 */
    private Long contractCount;

    /** 异常用量条数 */
    private Long abnormalCount;

    /** 异常占比（百分比，保留 2 位） */
    private BigDecimal abnormalRate;
}
