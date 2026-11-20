package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户退款单对象 crm_customer_refund
 *
 * <p>审批通过即扣减账户余额并写退款流水；已退款不可撤销。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerRefund extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 退款单号 */
    private String refundNo;

    /** 客户ID */
    private Long customerId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款方式：1原路退回 2其他方式 */
    private Integer refundType;

    /** 收款户名 */
    private String receiveName;

    /** 收款账号 */
    private String receiveAccount;

    /** 开户行 */
    private String receiveBank;

    /** 退款原因 */
    private String reason;

    /** 状态：0待审批 1已退款 2已驳回 */
    private Integer status;

    /** 审批人 */
    private Long auditor;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审批意见 */
    private String auditRemark;

    /** 申请人 */
    private Long creator;
}
