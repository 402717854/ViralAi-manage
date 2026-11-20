package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 资金账实校验结果
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundCheckVo
{
    /** 账户ID */
    private Long accountId;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 账户余额 */
    private BigDecimal balance;

    /** 流水汇总（入账 - 出账） */
    private BigDecimal flowSum;

    /** 差额 = 余额 - 流水汇总 */
    private BigDecimal diff;
}
