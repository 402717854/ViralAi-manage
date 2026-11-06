package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单生成结果
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBillGenerateResultVo
{
    /** 账期 */
    private String billCycle;

    /** 渠道ID */
    private Long supplierId;

    /** 新生成账单数 */
    private Integer billCount;

    /** 归集用量条数 */
    private Integer usageCount;

    /** 应收合计 */
    private BigDecimal totalSellAmount;

    /** 成本合计 */
    private BigDecimal totalCostAmount;

    /** 毛利合计 */
    private BigDecimal totalGrossProfit;

    /** 因已存在账单而跳过的合同数 */
    private Integer skippedCount;

    /** 新生成账单列表 */
    private List<CrmContractBillDto> bills;
}
