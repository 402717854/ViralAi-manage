package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单生成请求对象
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBillGenerateVo
{
    /** 渠道ID，必填 */
    private Long supplierId;

    /** 账期，格式 yyyy-MM，必填 */
    private String billCycle;

    /** 备注 */
    private String remark;
}
