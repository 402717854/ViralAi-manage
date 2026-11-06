package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同对账账单对象 crm_contract_bill
 *
 * <p>按「渠道 + 账期」生成，内部按合同分组汇总。账单生成后归属的 usage 回填
 * {@code bill_id} 并被锁定，不再参与后续重新生成。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractBill extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账单编号 BL-{渠道ID}-{yyyyMM}-{4位序列} */
    private String billNo;

    /** 合同ID */
    private Long contractId;

    /** 客户ID */
    private Long customerId;

    /** 渠道ID */
    private Long supplierId;

    /**
     * 渠道账户ID，0 表示未归属。
     *
     * <p>当前账单口径为「合同 + 渠道 + 账期」跨账户汇总，生成时固定写 0。
     * 若改为按账户出账，需同步改造分组、账单号规则与 uk_bill 口径。</p>
     */
    private Long supplierAccountId;

    /** 账期，格式 yyyy-MM */
    private String billCycle;

    /** 周期起始 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 周期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 总token */
    private Long totalTokens;

    /** 应收客户总额 */
    private BigDecimal totalSellAmount;

    /** 渠道总成本 */
    private BigDecimal totalCostAmount;

    /** 总毛利 */
    private BigDecimal totalGrossProfit;

    /** 账单状态，见 ContractBillStatusEnum */
    private Integer billStatus;

    /** 复核人 */
    private Long auditor;

    /** 复核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 创建人 */
    private Long creator;
}
