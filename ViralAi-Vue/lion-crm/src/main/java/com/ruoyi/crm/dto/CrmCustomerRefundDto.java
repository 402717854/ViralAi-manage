package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户退款单传输对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerRefundDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 退款单号 */
    @Excel(name = "退款单号", width = 24)
    private String refundNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    @Excel(name = "客户", width = 24)
    private String customerName;

    /** 退款金额 */
    @Excel(name = "退款金额", scale = 2)
    private BigDecimal amount;

    /** 退款方式 */
    private Integer refundType;

    /** 退款方式名称 */
    @Excel(name = "退款方式", width = 14)
    private String refundTypeName;

    /** 收款户名 */
    private String receiveName;

    /** 收款账号 */
    private String receiveAccount;

    /** 开户行 */
    private String receiveBank;

    /** 退款原因 */
    @Excel(name = "退款原因", width = 30)
    private String reason;

    /** 状态：0待审批 1已退款 2已驳回 */
    private Integer status;

    /** 状态名称 */
    @Excel(name = "状态", width = 12)
    private String statusName;

    /** 审批人 */
    private Long auditor;

    /** 审批人姓名 */
    @Excel(name = "审批人", width = 14)
    private String auditorName;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 22)
    private Date auditTime;

    /** 审批意见 */
    private String auditRemark;

    /** 申请人 */
    private Long creator;

    /** 申请人姓名 */
    @Excel(name = "申请人", width = 14)
    private String creatorName;
}
