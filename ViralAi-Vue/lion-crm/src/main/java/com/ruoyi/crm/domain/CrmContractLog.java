package com.ruoyi.crm.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 合同操作日志对象 crm_contract_log
 *
 * <p>所有状态变更与明细维护均写入该表；审批记录 = action 为 AUDIT_PASS / AUDIT_REJECT 的日志。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 合同ID */
    private Long contractId;

    /** 操作类型 */
    private String action;

    /** 操作名称 */
    private String actionName;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作内容（审批意见 / 作废原因 / 变更摘要） */
    private String content;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
