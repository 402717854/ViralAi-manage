package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户关联渠道对象
 * <p>
 * 用于客户详情与编辑页回显：只暴露渠道的基础信息，不返回渠道的联系人等敏感字段。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmCustomerSupplierDto
{
    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    private String supplierName;

    /** 渠道编码 */
    private String supplierCode;

    /** 渠道状态 0停用 1启用 */
    private Integer status;
}
