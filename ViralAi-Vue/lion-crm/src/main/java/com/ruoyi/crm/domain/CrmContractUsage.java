package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同用量对账明细对象 crm_contract_usage
 *
 * <p>一条网关流水匹配后生成一条记录，是账单、统计的主表。记录本身是**快照**：
 * 合同后续修改折扣或模型调整基准价，都不会重算历史用量（需求文档第八章）。</p>
 *
 * <p>差异调账通过新增一条 {@code usageType=1} 的冲销记录实现，冲销记录不占用
 * {@code call_log_id} 唯一索引（为空），因此可与原始流水共存。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractUsage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 网关流水ID；调账冲销为空 */
    private Long callLogId;

    /** 网关流水号；调账冲销为 ADJ-{原流水号} */
    private String callNo;

    /** 匹配到的合同ID，匹配失败为空 */
    private Long contractId;

    /** 合同明细ID */
    private Long contractProductId;

    /** 客户ID */
    private Long customerId;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道账户ID（crm_supplier_account.id），0 表示流水未归属账户 */
    private Long supplierAccountId;

    /** 模型产品ID */
    private Long productId;

    /** 模型编码 */
    private String productCode;

    /** 消耗token（冲销为负） */
    private Long totalTokens;
    /** 消耗金额*/
    private BigDecimal totalFees;

    /** 模型基准售价（每千token） */
    private BigDecimal baseSellPrice;

    /** 合同售卖折扣 */
    private BigDecimal sellDiscount;

    /** 本次向客户应收金额 */
    private BigDecimal sellAmount;

    /** 模型基准成本价（每千token） */
    private BigDecimal baseCostPrice;

    /** 合同成本折扣 */
    private BigDecimal costDiscount;

    /** 渠道成本金额 */
    private BigDecimal costAmount;

    /** 毛利 = 应收 - 成本 */
    private BigDecimal grossProfit;

    /** 匹配状态，见 UsageMatchStatusEnum */
    private Integer matchStatus;

    /** 匹配说明 */
    private String matchMsg;

    /** 调用时间（计费归属时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 归属账单ID，生成账单后回填 */
    private Long billId;

    /** 用量类型 0网关计费 1调账冲销 */
    private Integer usageType;

    /** 冲销对象用量ID */
    private Long refUsageId;
}
