package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 对账差异记录对象 crm_contract_bill_diff
 *
 * <p>对账匹配失败时自动生成，进入人工处理流程：
 * 调账（生成冲销用量）/ 豁免（不计费）/ 驳回（继续核查）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractBillDiff extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账单ID，未生成账单时为空 */
    private Long billId;

    /** 异常用量记录ID */
    private Long usageId;

    /** 差异类型，见 BillDiffTypeEnum */
    private Integer diffType;

    /** 差异描述 */
    private String diffDesc;

    /** 渠道ID */
    private Long supplierId;

    /** 客户ID */
    private Long customerId;

    /** 网关流水号 */
    private String callNo;

    /** 调用时间（归属账期） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 涉及金额（应收） */
    private BigDecimal amount;

    /** 处理状态，见 BillDiffHandleStatusEnum */
    private Integer handleStatus;

    /** 处理意见 */
    private String handleRemark;

    /** 处理人 */
    private Long handler;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
}
