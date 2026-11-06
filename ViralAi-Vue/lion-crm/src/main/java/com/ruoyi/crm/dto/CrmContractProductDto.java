package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同明细传输对象
 *
 * <p>既作为明细保存入参，也作为详情返回结构（此时额外回填 productName / productNo）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractProductDto
{
    /** 明细主键 */
    private Long id;

    /** 合同ID */
    private Long contractId;

    /** 渠道ID，明细行必须指定（须属于合同客户的已关联渠道） */
    private Long supplierId;

    /** 渠道名称（回显） */
    private String supplierName;

    /** 渠道账户ID：该行实际使用的账户，必须属于上面的渠道，必填 */
    private Long supplierAccountId;

    /** 渠道账户名（回显） */
    private String supplierAccountName;

    /** 渠道账户类型 1测试账户 2真实账户（回显） */
    private Integer supplierAccountType;

    /** 模型产品ID */
    private Long productId;

    /** 模型厂商名称（回显） */
    private String productName;

    /** 模型型号（回显） */
    private String productNo;

    /** 模型类型 1文本 2多模态 3图片处理 4音频处理 5视频处理（回显） */
    private Integer productType;

    /** 成本折扣 0.01~1.00 */
    private BigDecimal costDiscount;

    /** 售卖折扣 0.01~1.00 */
    private BigDecimal sellDiscount;

    /** 价格明细 JSON 字符串 */
    private String priceDetail;

    /** 备注 */
    private String remark;
}
