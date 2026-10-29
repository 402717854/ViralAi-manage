package com.ruoyi.crm.domain;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 供应商产品关联对象 crm_supplier_project
 * 
 * @author ruoyi
 * @date 2026-10-29
 */
@Getter
@Setter
public class CrmSupplierProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** $column.columnComment */
    private Long id;

    /** 供应商ID */
    private Long supplierId;

    /** 模型ID */
    private Long projectId;

    /** 最小成本折扣 */
    private BigDecimal minCostDiscount;

    /** 最大成本折扣 */
    private BigDecimal maxCostDiscount;

    /** 最小售价折扣 */
    private BigDecimal minSellDiscount;

    /** 最大售价折扣 */
    private BigDecimal maxSellDiscount;

    /** 最小代理折扣 */
    private BigDecimal minAgentDiscount;

    /** 最大代理折扣 */
    private BigDecimal maxAgentDiscount;

    /** 预计日消耗金额 */
    private BigDecimal dailyUsageAmount;

    /** $column.columnComment */
    private Long creator;

    /** $column.columnComment */
    private Long updater;

}
