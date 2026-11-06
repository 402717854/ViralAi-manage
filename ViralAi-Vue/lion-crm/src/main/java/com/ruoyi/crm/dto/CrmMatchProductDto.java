package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同匹配候选（生效合同明细行）
 *
 * <p>匹配唯一性判定以「合同明细行」为粒度：同一客户 + 同一渠道 + 同一账户 + 同一模型，
 * 同一时间只允许一行生效明细。候选多于一行即判定为脏数据，不做自动择一，
 * 由差异工作台人工处理。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Getter
@Setter
public class CrmMatchProductDto
{
    /** 合同明细ID */
    private Long contractProductId;

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    private String contactNo;

    /** 渠道ID（合同明细行） */
    private Long supplierId;

    /** 渠道账户ID（合同明细行，可为空表示历史未归属） */
    private Long supplierAccountId;

    /** 合同售卖折扣 */
    private BigDecimal sellDiscount;

    /** 合同成本折扣 */
    private BigDecimal costDiscount;
}
