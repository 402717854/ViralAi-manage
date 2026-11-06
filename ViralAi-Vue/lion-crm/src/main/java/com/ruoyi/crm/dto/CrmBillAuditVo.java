package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单复核请求对象
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBillAuditVo
{
    /** 账单ID */
    private Long billId;

    /** 复核结果：2 已确认 / 4 差异挂起 */
    private Integer billStatus;

    /** 复核意见 / 差异说明 */
    private String remark;
}
