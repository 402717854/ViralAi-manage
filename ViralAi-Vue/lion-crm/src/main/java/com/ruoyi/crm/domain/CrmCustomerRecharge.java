package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户充值单对象 crm_customer_recharge
 *
 * <p>客户打款到平台后由业务人员登记，财务审核通过才入账。
 * 已入账的充值单不可编辑、不可删除，纠错只能走人工调账或退款。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerRecharge extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 充值单号 */
    private String rechargeNo;

    /** 客户ID */
    private Long customerId;

    /** 充值金额 */
    private BigDecimal amount;

    /** 付款方式：1对公转账 2在线支付 3现金 4其他 */
    private Integer payType;

    /** 付款方名称 */
    private String payerName;

    /** 付款账号（建议只存后四位） */
    private String payerAccount;

    /** 到账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 银行流水号 */
    private String bankSerialNo;

    /** 凭证附件地址 */
    private String voucherUrl;

    /** 状态：0待审核 1已入账 2已驳回 */
    private Integer status;

    /** 审核人 */
    private Long auditor;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 审核意见 */
    private String auditRemark;

    /** 登记人 */
    private Long creator;
}
