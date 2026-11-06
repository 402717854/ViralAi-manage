package com.ruoyi.crm.domain;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * 网关流水上游标识映射 crm_gateway_pull_map
 *
 * <p>拉取回来的流水用的是上游自己的标识，本系统要用自己的主键，中间靠这张表对照：</p>
 * <ul>
 *   <li>{@code CUSTOMER}：上游用户名 → {@code crm_customer.id}。上游没有客户维度时
 *       （如 pixverse），把 {@code upstreamKey} 置为空串，表示「该账户的默认客户」；</li>
 *   <li>{@code PRODUCT}：上游模型标识 → {@code crm_model_product.product_code}。
 *       pixverse 的模型还要带清晰度与能力，用 {@code upstreamVariant} 承载，格式
 *       {@code quality|creation_type}（如 {@code 720p|text_to_video}）。</li>
 * </ul>
 *
 * <p>匹配优先级：账户级精确 → 渠道级精确 → 账户级忽略变体 → 渠道级忽略变体。
 * {@code supplierAccountId = 0} 表示该规则对该渠道下所有账户生效。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Getter
@Setter
public class CrmGatewayPullMap
{
    /** 映射类型：客户 */
    public static final String TYPE_CUSTOMER = "CUSTOMER";

    /** 映射类型：模型编码 */
    public static final String TYPE_PRODUCT = "PRODUCT";

    /** 主键 */
    private Long id;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道账户ID，0 表示该渠道通用 */
    private Long supplierAccountId;

    /** 映射类型 CUSTOMER / PRODUCT */
    private String mapType;

    /** 上游标识：CUSTOMER 为用户名（空串表示账户默认客户），PRODUCT 为上游模型标识 */
    private String upstreamKey;

    /** 上游变体，如 720p|text_to_video；无变体为空串 */
    private String upstreamVariant;

    /** 目标ID：CUSTOMER 时为 crm_customer.id */
    private Long targetId;

    /** 目标编码：PRODUCT 时为 crm_model_product.product_code */
    private String targetCode;

    /** 状态 0失效 1有效 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建者 */
    private Long creator;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
