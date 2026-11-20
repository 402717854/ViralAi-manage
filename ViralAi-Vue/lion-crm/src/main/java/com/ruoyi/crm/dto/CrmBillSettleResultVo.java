package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单结算结果对象
 *
 * <p>结算接口不再返回裸影响行数：前端需要展示「扣了多少、扣完还剩多少」，
 * 否则用户点完结算只能靠猜。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmBillSettleResultVo
{
    /** 账单ID */
    private Long billId;

    /** 账单编号 */
    private String billNo;

    /** 结算方式：1资金扣减 2线下收款 */
    private Integer settleType;

    /** 账单应付金额 */
    private BigDecimal billAmount;

    /** 本次扣减金额（负数账单为退回金额的绝对值） */
    private BigDecimal deductAmount;

    /** 扣减前余额（线下收款时为空） */
    private BigDecimal balanceBefore;

    /** 扣减后余额（线下收款时为空） */
    private BigDecimal balanceAfter;

    /** 资金流水ID（线下收款时为空） */
    private Long fundFlowId;

    /** 资金流水号 */
    private String fundFlowNo;

    /** 结算时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date settleTime;
}
