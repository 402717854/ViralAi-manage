package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单分组汇总（按合同聚合的用量结果，仅服务内部使用）
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBillGroupDto
{
    /** 合同ID */
    private Long contractId;

    /** 客户ID */
    private Long customerId;

    /** 渠道ID */
    private Long supplierId;

    /** 总token */
    private Long totalTokens;

    /** 应收总额 */
    private BigDecimal totalSellAmount;

    /** 成本总额 */
    private BigDecimal totalCostAmount;

    /** 毛利总额 */
    private BigDecimal totalGrossProfit;

    /** 用量条数 */
    private Integer usageCount;
}
