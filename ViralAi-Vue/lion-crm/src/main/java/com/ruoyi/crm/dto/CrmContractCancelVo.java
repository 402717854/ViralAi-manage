package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同作废入参
 *
 * <p>作废原因为必填项，写入 crm_contract.cancel_reason 与操作日志。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractCancelVo
{
    /** 合同ID */
    private Long contractId;

    /** 作废原因 */
    private String cancelReason;
}
