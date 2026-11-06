package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同操作入参（提交审批、归档等仅需合同ID的操作）
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractOperateVo
{
    /** 合同ID */
    private Long contractId;
}
