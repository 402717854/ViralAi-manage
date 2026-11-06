package com.ruoyi.crm.domain;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 合同模型产品明细对象 crm_contract_product
 *
 * <p>supplier_id 冗余自合同主表，用于归属强校验与结算隔离；
 * price_detail 为 JSON 字符串，承载计价单位、基准售价、配额等可扩展计费字段。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 合同ID */
    private Long contractId;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道账户ID（crm_supplier_account.id），必填，标识该行使用的具体账户 */
    private Long supplierAccountId;

    /** 模型产品ID */
    private Long productId;

    /** 成本折扣 0.01~1.00 */
    private BigDecimal costDiscount;

    /** 售卖折扣 0.01~1.00 */
    private BigDecimal sellDiscount;

    /** 价格明细 JSON */
    private String priceDetail;

    /** 创建者 */
    private Long creator;

    /** 更新者 */
    private Long updater;

    /** 是否删除 0否 1是 */
    private Integer deleted;
}
