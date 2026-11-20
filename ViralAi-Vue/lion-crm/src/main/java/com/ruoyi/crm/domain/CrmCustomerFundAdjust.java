package com.ruoyi.crm.domain;

import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金人工调账单 crm_customer_fund_adjust
 *
 * <p>调账本身没有业务来源单据，但资金流水需要可追溯的 biz_id（唯一键的一部分），
 * 因此单独建表生成调账单号，避免拿易变数值硬凑 biz_id。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerFundAdjust extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 调账单号 */
    private String adjustNo;

    /** 客户ID */
    private Long customerId;

    /** 调账金额（正数） */
    private BigDecimal amount;

    /** 方向：1调增 2调减 */
    private Integer direction;

    /** 调账原因 */
    private String reason;

    /** 操作人 */
    private Long creator;
}
