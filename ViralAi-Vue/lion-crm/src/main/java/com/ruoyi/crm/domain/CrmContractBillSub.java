package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同对账子账单对象 crm_contract_bill_sub
 *
 * <p>父账单 {@code crm_contract_bill} 粒度为「合同 + 渠道 + 账期」，本实体按「父账单 + 自然日」
 * 拆分金额，用于按天查看应收、成本与毛利。金额口径与父账单一致：只统计匹配成功的用量，
 * 归属日期取 {@code crm_contract_usage.call_time} 的自然日。</p>
 *
 * <p>子账单不独立维护账单状态，复核与结算一律以父账单为准，避免双写不一致。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmContractBillSub extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 子账单编号 {父账单号}-D{yyyyMMdd} */
    private String subBillNo;

    /** 父账单ID */
    private Long billId;

    /** 父账单编号，冗余便于列表直接检索 */
    private String billNo;

    /** 账期 yyyy-MM，冗余便于按账期聚合 */
    private String billCycle;

    /** 账单日期，取用量 call_time 的自然日 */
    private Date billDate;

    /** 合同ID */
    private Long contractId;

    /** 客户ID */
    private Long customerId;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道账户ID，0 表示未归属（与父账单口径一致，当前固定写 0） */
    private Long supplierAccountId;

    /** 归集用量条数 */
    private Integer usageCount;

    /** 当日总token */
    private Long totalTokens;

    /** 当日应收客户金额 */
    private BigDecimal totalSellAmount;

    /** 当日渠道成本 */
    private BigDecimal totalCostAmount;

    /** 当日毛利 = 应收 - 成本 */
    private BigDecimal totalGrossProfit;

    /** 创建人 */
    private Long creator;
}
