package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * CRM 商机状态选项
 *
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmBusStatusOptionDto
{
    /** 状态码 */
    private Long code;

    /** 状态名称 */
    private String info;

    public CrmBusStatusOptionDto()
    {
    }

    public CrmBusStatusOptionDto(Long code, String info)
    {
        this.code = code;
        this.info = info;
    }
}
