package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 退款申请请求对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundRefundVo
{
    /** 客户ID */
    private Long customerId;

    /** 退款金额，必须大于 0 且不超过可用余额 */
    private BigDecimal amount;

    /** 退款方式：1原路退回 2其他方式 */
    private Integer refundType;

    /** 收款户名 */
    private String receiveName;

    /** 收款账号 */
    private String receiveAccount;

    /** 开户行 */
    private String receiveBank;

    /** 退款原因，必填 */
    private String reason;
}
