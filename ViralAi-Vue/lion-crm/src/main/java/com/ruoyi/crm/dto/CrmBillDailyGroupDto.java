package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * 子账单按天聚合结果（仅服务账单生成，内部使用）
 *
 * <p>父账单归集用量后，按 {@code date(call_time)} 分组，为每一天生成一条子账单。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmBillDailyGroupDto
{
    /** 账单日期 */
    private Date billDate;

    /** 用量条数 */
    private Integer usageCount;

    /** 当日总token */
    private Long totalTokens;

    /** 当日应收金额 */
    private BigDecimal totalSellAmount;

    /** 当日成本 */
    private BigDecimal totalCostAmount;

    /** 当日毛利 */
    private BigDecimal totalGrossProfit;
}
