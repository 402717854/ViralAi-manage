package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同维度消耗排行
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractRankVo
{
    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    private String contactNo;

    /** 合同名称 */
    private String contractName;

    /** 客户名称 */
    private String customerName;

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
}
