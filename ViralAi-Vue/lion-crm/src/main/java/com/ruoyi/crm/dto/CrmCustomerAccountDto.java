package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金账户传输对象
 *
 * <p>查询入参：customerName / ownerUser / status / hasBalance / minBalance / maxBalance，
 * 数据权限通过 params.ownerUser 注入（客户负责人维度）。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerAccountDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 账户ID */
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 客户名称（查询条件 + 展示） */
    @Excel(name = "客户", width = 24)
    private String customerName;

    /** 客户负责人（账号ID字符串，与 crm_customer.owner_user 一致） */
    private String ownerUser;

    /** 客户负责人姓名 */
    @Excel(name = "负责人", width = 14)
    private String ownerName;

    /** 账面余额 */
    @Excel(name = "可用余额", scale = 2)
    private BigDecimal balance;

    /** 冻结金额 */
    private BigDecimal frozenAmount;

    /** 可用余额（余额 - 冻结） */
    private BigDecimal availableBalance;

    /** 累计充值 */
    @Excel(name = "累计充值", scale = 2)
    private BigDecimal totalRecharge;

    /** 累计结算扣减 */
    @Excel(name = "累计扣减", scale = 2)
    private BigDecimal totalDeduct;

    /** 累计退款 */
    @Excel(name = "累计退款", scale = 2)
    private BigDecimal totalRefund;

    /** 最近变动时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastFlowTime;

    /** 账户状态：1正常 0冻结 */
    private Integer status;

    /** 查询条件：1 仅有余额 0 仅零余额，空为不限 */
    private Integer hasBalance;

    /** 查询条件：余额下限 */
    private BigDecimal minBalance;

    /** 查询条件：余额上限 */
    private BigDecimal maxBalance;
}
