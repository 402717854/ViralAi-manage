package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 供应商产品关联明细对象（含模型信息）
 * <p>
 * 用于按供应商编号查询其产品关联，连带返回模型型号等展示字段，
 * 避免前端再按 projectId 二次查询模型。
 *
 * @author ruoyi
 * @date 2026-10-29
 */
@Getter
@Setter
public class CrmSupplierProjectDto
{
    /** 模型ID（crm_supplier_project.project_id） */
    private Long projectId;

    /** 模型型号（crm_model_product.no） */
    private String projectNo;

    /** 模型厂商（crm_model_product.name） */
    private String projectName;

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
}
