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

    /** 供应商ID（本条报价的归属方，产品明细的维护主体） */
    private Long supplierId;

    /**
     * 模型所属供应商ID
     * <p>
     * 这个模型取自哪个供应商（渠道）的模型池。与 {@link #supplierId} 可以不同：
     * 供应商 A 的报价单里可以挂 B 的 gpt-4o，再按 A 的口径定义折扣。
     * 为空视为当前供应商自身，由服务层落库时回填。
     */
    private Long projectSupplierId;

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

    /** $column.columnComment */
    private Long creator;

    /** $column.columnComment */
    private Long updater;

}
