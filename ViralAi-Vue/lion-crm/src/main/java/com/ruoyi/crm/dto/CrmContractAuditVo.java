package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同审批入参
 *
 * <p>auditResult：1 通过（转已签约并写入签约时间）；2 驳回（退回草稿）。驳回时审批意见必填。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractAuditVo
{
    /** 合同ID */
    private Long contractId;

    /** 审批结果 1通过 2驳回 */
    private Integer auditResult;

    /** 审批意见 */
    private String auditOpinion;
}
