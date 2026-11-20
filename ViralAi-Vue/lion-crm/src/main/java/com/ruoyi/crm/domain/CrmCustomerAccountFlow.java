package com.ruoyi.crm.domain;

import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金流水对象 crm_customer_account_flow
 *
 * <p>只增不改：任何错误都必须用反向流水冲正，不允许修改历史流水，
 * 否则账实校验与审计都会失去意义。</p>
 *
 * <p>{@code (biz_type, biz_id)} 唯一：一张账单只可能有一条结算扣款流水，
 * 这是结算幂等的数据库层保证。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerAccountFlow extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 流水号 */
    private String flowNo;

    /** 账户ID */
    private Long accountId;

    /** 客户ID */
    private Long customerId;

    /** 业务类型，见 CustomerFundBizTypeEnum */
    private Integer bizType;

    /** 业务单据ID */
    private Long bizId;

    /** 业务单据编号 */
    private String bizNo;

    /** 方向：1入账 2出账 */
    private Integer direction;

    /** 变动金额（恒为正数） */
    private BigDecimal amount;

    /** 变动前余额 */
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    /** 关联账单ID */
    private Long billId;

    /** 关联账单编号 */
    private String billNo;

    /** 关联合同ID */
    private Long contractId;

    /** 操作人 */
    private Long operator;

    /** 流水生成时间取 BaseEntity.createTime，避免字段遮蔽父类 */
}
