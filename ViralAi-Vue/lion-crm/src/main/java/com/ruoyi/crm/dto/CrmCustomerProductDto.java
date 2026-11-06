package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户可用的渠道模型对象
 * <p>
 * 在模型产品字段之外附带所属渠道，供商机/合同明细在「客户已开通渠道」范围内选择模型。
 * 同一模型被多个渠道上架时会返回多行，由前端按渠道分组展示。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmCustomerProductDto extends CrmModelProductDto
{
    private static final long serialVersionUID = 1L;

    /** 所属渠道ID */
    private Long supplierId;

    /** 所属渠道名称 */
    private String supplierName;
}
