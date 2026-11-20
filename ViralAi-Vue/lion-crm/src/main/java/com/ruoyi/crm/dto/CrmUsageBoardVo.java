package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 渠道对账看板数据
 *
 * <p>概览卡片为账期全量合计；明细区以「客户 + 渠道」为统计基准，另附渠道成本排行。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmUsageBoardVo
{
    /** 统计账期 yyyy-MM */
    private String billCycle;

    /** 概览：调用次数 */
    private Long totalCalls;

    /** 概览：总token */
    private Long totalTokens;

    /** 概览：应收总额 */
    private BigDecimal totalSellAmount;

    /** 概览：成本总额 */
    private BigDecimal totalCostAmount;

    /** 概览：毛利总额 */
    private BigDecimal totalGrossProfit;

    /** 概览：毛利率（百分比） */
    private BigDecimal profitRate;

    /** 概览：异常用量条数 */
    private Long abnormalCount;

    /** 概览：异常占比（百分比） */
    private BigDecimal abnormalRate;

    /** 概览：已生成账单数 */
    private Integer billCount;

    /** 概览：待处理差异数 */
    private Integer pendingDiffCount;

    /** 客户 + 渠道维度汇总（按客户与渠道组合聚合） */
    private List<CrmCustomerSupplierStatVo> customerSupplierStats;

    /** 渠道成本排行（按渠道成本倒序，取前 10） */
    private List<CrmSupplierCostRankVo> supplierCostRank;
}
