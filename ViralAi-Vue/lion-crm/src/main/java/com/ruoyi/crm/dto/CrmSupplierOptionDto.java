package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * CRM 供应商下拉选项对象
 * <p>
 * 仅暴露 id 与 name，用于供应商选择器等轻量场景，避免返回完整供应商实体。
 *
 * @author ruoyi
 * @date 2026-10-27
 */
@Getter
@Setter
public class CrmSupplierOptionDto
{
    /** 供应商编号 */
    private Long id;

    /** 供应商名称 */
    private String name;
}
