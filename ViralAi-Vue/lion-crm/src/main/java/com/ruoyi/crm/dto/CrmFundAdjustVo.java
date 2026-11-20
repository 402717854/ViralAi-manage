package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 人工调账请求对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundAdjustVo
{
    /** 客户ID */
    private Long customerId;

    /** 调账金额，恒为正数，方向由 direction 决定 */
    private BigDecimal amount;

    /** 方向：1调增 2调减 */
    private Integer direction;

    /** 调账原因，必填 */
    private String reason;
}
