package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 充值单 / 退款单审核请求对象
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundAuditVo
{
    /** 单据ID（充值单或退款单） */
    private Long id;

    /** 是否通过：true 通过、false 驳回 */
    private Boolean pass;

    /** 审核意见，驳回时必填 */
    private String remark;
}
