package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型报价渠道对象（按模型反查报过价的渠道）
 * <p>
 * 供应商报价明细选中「模型型号」后，按 project_id 懒加载该模型在
 * crm_supplier_project 中出现过的渠道（supplier_id 关联 crm_supplier），
 * 前端据此级联「模型所属供应商」并带出该渠道的折扣区间；
 * 查不到任何渠道时前端按「当前供应商」处理。
 *
 * @author ruoyi
 * @date 2026-11-19
 */
@Getter
@Setter
public class CrmProjectChannelDto
{
    /** 报价渠道ID（crm_supplier_project.supplier_id） */
    private Long supplierId;

    /** 报价渠道名称（crm_supplier.name） */
    private String supplierName;

    /** 模型所属供应商ID（crm_supplier_project.project_supplier_id） */
    private Long projectSupplierId;

    /** 模型所属供应商名称 */
    private String projectSupplierName;

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
}
