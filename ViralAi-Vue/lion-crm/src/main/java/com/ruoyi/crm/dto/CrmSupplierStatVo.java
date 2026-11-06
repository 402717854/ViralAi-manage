package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 渠道维度对账统计
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmSupplierStatVo
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

    /** 毛利率（百分比，保留 2 位） */
    private BigDecimal profitRate;

    /** 参与计费的合同数 */
    private Long contractCount;

    /** 异常用量条数 */
    private Long abnormalCount;

    /** 异常占比（百分比，保留 2 位） */
    private BigDecimal abnormalRate;
}
