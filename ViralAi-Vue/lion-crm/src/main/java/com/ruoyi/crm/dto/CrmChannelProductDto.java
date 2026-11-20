package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 渠道可售模型对象（渠道 × 已上架模型）
 * <p>
 * 在模型产品字段之外附带所属渠道，作为商机与合同明细选择模型的数据源。
 * 客户与渠道的绑定不再在客户资料中维护，改由商机/合同明细行承载：
 * 每一行同时记录渠道、模型与渠道账户，保存时由服务端做强制性一致性校验。
 * 同一模型被多个渠道上架时会返回多行，由前端按渠道分组展示。
 *
 * @author ruoyi
 * @date 2026-11-19
 */
@Getter
@Setter
public class CrmChannelProductDto extends CrmModelProductDto
{
    private static final long serialVersionUID = 1L;

    /** 所属渠道ID */
    private Long supplierId;

    /** 所属渠道名称 */
    private String supplierName;
}
