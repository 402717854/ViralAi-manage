package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 充值登记请求对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundRechargeVo
{
    /** 客户ID */
    private Long customerId;

    /** 充值金额，必须大于 0 */
    private BigDecimal amount;

    /** 付款方式：1对公转账 2在线支付 3现金 4其他 */
    private Integer payType;

    /** 付款方名称（允许与客户名称不一致，代付场景） */
    private String payerName;

    /** 付款账号（建议只存后四位） */
    private String payerAccount;

    /** 到账时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 银行流水号，同一客户下唯一 */
    private String bankSerialNo;

    /** 凭证附件地址 */
    private String voucherUrl;

    /** 备注 */
    private String remark;
}
