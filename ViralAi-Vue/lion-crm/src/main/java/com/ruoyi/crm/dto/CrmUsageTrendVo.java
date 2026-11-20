package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 用量趋势（按账期聚合）
 *
 * <p>口径与看板概览完全一致：调用数与异常数含匹配失败的用量，
 * token 与金额只统计 {@code match_status = 1} 的用量，避免趋势线出现虚高。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmUsageTrendVo
{
    /** 账期 yyyy-MM */
    private String billCycle;

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

    /** 毛利率（百分比） */
    private BigDecimal profitRate;

    /** 异常用量条数 */
    private Long abnormalCount;

    /** 异常占比（百分比） */
    private BigDecimal abnormalRate;

    /**
     * 空账期占位：区间内没有任何用量时补零，保证趋势图横轴连续
     *
     * @param billCycle 账期 yyyy-MM
     * @return 全零趋势行
     */
    public static CrmUsageTrendVo emptyOf(String billCycle)
    {
        CrmUsageTrendVo vo = new CrmUsageTrendVo();
        vo.setBillCycle(billCycle);
        vo.setTotalCalls(0L);
        vo.setTotalTokens(0L);
        vo.setTotalSellAmount(BigDecimal.ZERO);
        vo.setTotalCostAmount(BigDecimal.ZERO);
        vo.setTotalGrossProfit(BigDecimal.ZERO);
        vo.setProfitRate(BigDecimal.ZERO);
        vo.setAbnormalCount(0L);
        vo.setAbnormalRate(BigDecimal.ZERO);
        return vo;
    }
}
