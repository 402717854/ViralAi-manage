package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 结算预览对象
 *
 * <p>结算弹窗需要「应付金额 + 客户可用余额」，但余额属于资金模块。
 * 若前端直接调资金接口，会把账单页与 {@code crm:fund:list} 权限耦合：
 * 仅有结算权限的财务会拿到 403，页面显示余额 0，进而误判「余额不足」。
 * 因此由账单侧提供预览接口，权限只要求 {@code crm:bill:settle}。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmBillSettlePreviewVo
{
    /** 账单ID */
    private Long billId;

    /** 账单编号 */
    private String billNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 应付金额 */
    private BigDecimal billAmount;

    /** 客户可用余额 */
    private BigDecimal availableBalance;

    /** 是否已确认且可结算 */
    private Boolean settleable;
}
