package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户充值单传输对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerRechargeDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 充值单号 */
    @Excel(name = "充值单号", width = 24)
    private String rechargeNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    @Excel(name = "客户", width = 24)
    private String customerName;

    /** 充值金额 */
    @Excel(name = "充值金额", scale = 2)
    private BigDecimal amount;

    /** 付款方式 */
    private Integer payType;

    /** 付款方式名称 */
    @Excel(name = "付款方式", width = 14)
    private String payTypeName;

    /** 付款方名称 */
    @Excel(name = "付款方", width = 22)
    private String payerName;

    /** 付款账号 */
    private String payerAccount;

    /** 到账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "到账时间", width = 22)
    private Date payTime;

    /** 银行流水号 */
    @Excel(name = "银行流水号", width = 26)
    private String bankSerialNo;

    /** 凭证地址 */
    private String voucherUrl;

    /** 状态：0待审核 1已入账 2已驳回 */
    private Integer status;

    /** 状态名称 */
    @Excel(name = "状态", width = 12)
    private String statusName;

    /** 审核人 */
    private Long auditor;

    /** 审核人姓名 */
    @Excel(name = "审核人", width = 14)
    private String auditorName;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 22)
    private Date auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 登记人 */
    private Long creator;

    /** 登记人姓名 */
    @Excel(name = "登记人", width = 14)
    private String creatorName;
}
