package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金账户对象 crm_customer_account
 *
 * <p>一个客户一个账户，跨合同、跨渠道共用余额。账户独立成表而非在 crm_customer 加列：
 * 客户列表与下拉属于高频查询，且资金变动需锁账户行，不应连带锁客户主表。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 账面余额 */
    private BigDecimal balance;

    /** 冻结金额（当前未启用） */
    private BigDecimal frozenAmount;

    /** 累计充值 */
    private BigDecimal totalRecharge;

    /** 累计结算扣减 */
    private BigDecimal totalDeduct;

    /** 累计退款 */
    private BigDecimal totalRefund;

    /** 乐观锁版本号 */
    private Integer version;

    /** 状态：1正常 0冻结 */
    private Integer status;

    /** 最近一次资金变动时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFlowTime;

    /**
     * 可用余额 = 账面余额 - 冻结金额
     *
     * @return 可用余额
     */
    public BigDecimal available()
    {
        BigDecimal total = balance == null ? BigDecimal.ZERO : balance;
        BigDecimal frozen = frozenAmount == null ? BigDecimal.ZERO : frozenAmount;
        return total.subtract(frozen);
    }
}
