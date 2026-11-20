package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 资金变动指令（内部使用）
 *
 * <p>所有余额变动都必须通过 {@code CrmCustomerFundLedger.change()} 执行，
 * 由账本统一完成「锁账户 → 校验余额 → 写流水 → 更新余额」，
 * 禁止业务代码直接 update 余额，否则必然出现流水与余额不一致。</p>
 *
 * <p>方向由 {@code bizType} 唯一决定（见 {@code CustomerFundBizTypeEnum}），
 * 调用方只传正数金额，不传方向。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundChangeCmd
{
    /** 客户ID */
    private Long customerId;

    /** 业务类型编码，见 CustomerFundBizTypeEnum */
    private Integer bizType;

    /** 业务单据ID（唯一键的一部分，决定幂等） */
    private Long bizId;

    /** 业务单据编号，仅用于展示 */
    private String bizNo;

    /** 变动金额，恒为正数 */
    private BigDecimal amount;

    /** 关联账单ID */
    private Long billId;

    /** 关联账单编号 */
    private String billNo;

    /** 关联合同ID */
    private Long contractId;

    /** 备注 */
    private String remark;

    /** 操作人，为空时取登录用户，无登录上下文回退系统账号 */
    private Long operator;

    /**
     * 便捷构造
     *
     * @param customerId 客户ID
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     * @param amount 金额（正数）
     * @return 指令
     */
    public static CrmFundChangeCmd of(Long customerId, Integer bizType, Long bizId, BigDecimal amount)
    {
        CrmFundChangeCmd cmd = new CrmFundChangeCmd();
        cmd.setCustomerId(customerId);
        cmd.setBizType(bizType);
        cmd.setBizId(bizId);
        cmd.setAmount(amount);
        return cmd;
    }
}
