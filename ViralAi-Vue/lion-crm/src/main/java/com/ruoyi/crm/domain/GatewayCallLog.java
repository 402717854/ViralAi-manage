package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 模型网关调用流水对象 gateway_call_log
 *
 * <p>对账的源数据，网关可直接投递（MQ / HTTP 上报）或由对账任务定时拉取。
 * {@code callNo} 具备唯一索引，是入库幂等的唯一依据。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class GatewayCallLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 网关调用唯一流水号，幂等key */
    private String callNo;

    /** 客户ID，用于匹配合同 */
    private Long customerId;

    /** 渠道ID（模型归属渠道） */
    private Long supplierId;

    /** 渠道账户ID（crm_supplier_account.id），0 表示网关未上报/未归属 */
    private Long supplierAccountId;

    /** 模型编码，对应 crm_model_product.product_code */
    private String productCode;

    /** 模型类型 LLM/Embedding/TTS/Image */
    private String modelType;

    /** 输入token */
    private Long promptTokens;

    /** 输出token */
    private Long completionTokens;

    /** 总token */
    private Long totalTokens;

    /**总消耗费用*/
    private BigDecimal totalFees;

    /** 调用发生时间（计费归属时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 调用状态 0失败 1成功 */
    private Integer callStatus;

    /** 失败原因 */
    private String errMsg;

    /** 网关侧原始计费信息 */
    private String rawPriceDetail;

    /** 是否删除 */
    private Integer deleted;
}
