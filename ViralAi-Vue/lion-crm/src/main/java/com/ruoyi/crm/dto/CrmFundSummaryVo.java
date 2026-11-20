package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金汇总对象（列表页顶部汇总条）
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundSummaryVo
{
    /** 客户数（有账户的客户） */
    private Integer customerCount;

    /** 余额合计 */
    private BigDecimal totalBalance;

    /** 本月充值合计 */
    private BigDecimal monthRecharge;

    /** 本月扣减合计 */
    private BigDecimal monthDeduct;

    /** 本月退款合计 */
    private BigDecimal monthRefund;

    /** 待审核充值单数 */
    private Integer pendingRechargeCount;
}
