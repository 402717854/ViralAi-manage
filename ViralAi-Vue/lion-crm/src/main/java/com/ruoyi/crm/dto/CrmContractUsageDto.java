package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 用量对账台账传输对象
 *
 * <p>查询入参：callNo / customerId / supplierId / supplierAccountId / contractId / productCode /
 * matchStatus / billId / usageType，
 * 另支持 params.beginTime、params.endTime（调用时间范围）、params.matchStatusList（多选状态，
 * 逗号分隔）与 params.scopeSupplierIds（渠道数据权限）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractUsageDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 网关流水ID */
    private Long callLogId;

    /** 网关流水号 */
    @Excel(name = "流水号", width = 34)
    private String callNo;

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    @Excel(name = "合同编号", width = 26)
    private String contactNo;

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

    /** 模型产品ID */
    private Long productId;

    /** 模型编码 */
    @Excel(name = "模型编码", width = 22)
    private String productCode;

    /** 模型型号 */
    private String modelNo;

    /** 消耗token */
    @Excel(name = "token")
    private Long totalTokens;

    /** 消耗费用（网关上报金额快照） */
    @Excel(name = "消耗费用", scale = 8)
    private BigDecimal totalFees;

    /** 模型基准售价（每千token） */
    private BigDecimal baseSellPrice;

    /** 合同售卖折扣 */
    private BigDecimal sellDiscount;

    /** 应收金额 */
    @Excel(name = "应收金额", scale = 8)
    private BigDecimal sellAmount;

    /** 模型基准成本价（每千token） */
    private BigDecimal baseCostPrice;

    /** 合同成本折扣 */
    private BigDecimal costDiscount;

    /** 渠道成本金额 */
    @Excel(name = "成本金额", scale = 8)
    private BigDecimal costAmount;

    /** 毛利 */
    @Excel(name = "毛利", scale = 8)
    private BigDecimal grossProfit;

    /** 匹配状态 */
    @Excel(name = "匹配状态", readConverterExp = "0=待匹配,1=匹配成功,2=无生效合同,3=合同已过期,4=模型不在合同内,5=渠道不匹配,6=其他异常,7=多份生效合同")
    private Integer matchStatus;

    /** 匹配说明 */
    @Excel(name = "匹配说明", width = 40)
    private String matchMsg;

    /** 调用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调用时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 归属账单ID */
    private Long billId;

    /** 账单编号 */
    private String billNo;

    /** 用量类型 0网关计费 1调账冲销 */
    @Excel(name = "用量类型", readConverterExp = "0=网关计费,1=调账冲销")
    private Integer usageType;

    /** 冲销对象用量ID */
    private Long refUsageId;
}
