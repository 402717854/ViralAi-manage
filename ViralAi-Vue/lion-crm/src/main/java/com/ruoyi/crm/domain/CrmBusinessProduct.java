package com.ruoyi.crm.domain;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商机渠道模型产品关联对象 crm_business_product
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmBusinessProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 商机编号 */
    private Long busId;

    /** 渠道编号 */
    private Long supplierId;

    /** 模型产品编号 */
    private Long productId;

    /** 成本折扣 */
    private BigDecimal costDiscount;

    /** 售卖折扣 */
    private BigDecimal sellDiscount;

    /** 价格明细 */
    private String priceDetail;

    /** $column.columnComment */
    private Long creator;

    /** $column.columnComment */
    private Long updator;

}
