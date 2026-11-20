package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单结算请求对象
 *
 * <p>请求体可选：不传时按「资金扣减 + 应付全额」处理，兼容未升级的前端调用。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmBillSettleVo
{
    /** 结算方式：1资金扣减 2线下收款，空则默认 1 */
    private Integer settleType;

    /** 实结金额，空则按账单应付金额处理 */
    private BigDecimal settledAmount;

    /** 线下收款的收款方式（仅 settleType=2 使用） */
    private String payType;

    /** 线下收款的凭证号 / 流水号（仅 settleType=2 使用） */
    private String payVoucher;

    /** 结算备注 */
    private String remark;
}
