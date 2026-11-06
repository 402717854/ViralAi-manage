package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 网关调用流水列表传输对象
 *
 * <p>查询入参：callNo / customerId / supplierId / supplierAccountId / productCode / callStatus，
 * 另支持 params.beginTime、params.endTime（调用时间范围）与 params.scopeSupplierIds（数据权限）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class GatewayCallLogDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 网关调用流水号 */
    @Excel(name = "流水号", width = 34)
    private String callNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    @Excel(name = "客户", width = 20)
    private String customerName;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    @Excel(name = "渠道", width = 20)
    private String supplierName;

    /** 渠道账户ID */
    private Long supplierAccountId;

    /** 渠道账户名称 */
    @Excel(name = "渠道账户", width = 20)
    private String supplierAccountName;

    /** 模型编码 */
    @Excel(name = "模型编码", width = 22)
    private String productCode;

    /** 模型类型 */
    @Excel(name = "模型类型", width = 12)
    private String modelType;

    /** 输入token */
    @Excel(name = "输入token")
    private Long promptTokens;

    /** 输出token */
    @Excel(name = "输出token")
    private Long completionTokens;

    /** 总token */
    @Excel(name = "总token")
    private Long totalTokens;

    /** 消耗费用（网关上报金额） */
    @Excel(name = "消耗费用", scale = 8)
    private BigDecimal totalFees;

    /** 调用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调用时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 调用状态 0失败 1成功 */
    @Excel(name = "调用状态", readConverterExp = "0=失败,1=成功")
    private Integer callStatus;

    /** 失败原因 */
    private String errMsg;

    /** 网关侧原始计费信息 */
    private String rawPriceDetail;

    /** 是否已生成对账台账 0否 1是（列表回显） */
    private Integer reconciled;
}
