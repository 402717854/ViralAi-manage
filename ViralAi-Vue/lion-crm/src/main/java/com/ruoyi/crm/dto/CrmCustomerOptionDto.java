package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * CRM 客户下拉选项对象
 * <p>
 * 仅暴露 id 与 name，用于客户选择器等轻量场景，避免返回完整客户实体。
 *
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmCustomerOptionDto
{
    /** 客户编号 */
    private Long id;

    /** 客户名称 */
    private String name;
}
