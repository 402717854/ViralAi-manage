package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 对账账单传输对象
 *
 * <p>查询入参：billNo / contractId / customerId / supplierId / billCycle / billStatus，
 * 另支持 params.beginTime、params.endTime（创建时间范围）与 params.scopeSupplierIds（数据权限）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractBillDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账单编号 */
    @Excel(name = "账单编号", width = 26)
    private String billNo;

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    @Excel(name = "合同编号", width = 26)
    private String contactNo;

    /** 合同名称 */
    private String contractName;

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

    /** 账期 */
    @Excel(name = "账期")
    private String billCycle;

    /** 周期起始 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 周期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 总token */
    @Excel(name = "总token")
    private Long totalTokens;

    /** 应收总额 */
    @Excel(name = "应收总额", scale = 8)
    private BigDecimal totalSellAmount;

    /** 总成本 */
    @Excel(name = "总成本", scale = 8)
    private BigDecimal totalCostAmount;

    /** 总毛利 */
    @Excel(name = "总毛利", scale = 8)
    private BigDecimal totalGrossProfit;

    /** 账单状态 */
    @Excel(name = "账单状态", readConverterExp = "0=待生成,1=待复核,2=已确认,3=已结算,4=差异挂起")
    private Integer billStatus;

    /** 结算方式：0未结算 1资金扣减 2线下收款 */
    @Excel(name = "结算方式", readConverterExp = "0=未结算,1=资金扣减,2=线下收款")
    private Integer settleType;

    /** 实结金额 */
    @Excel(name = "实结金额", scale = 8)
    private BigDecimal settledAmount;

    /** 关联资金流水ID */
    private Long fundFlowId;

    /** 关联资金流水号（列表不回填，详情按需查询） */
    private String fundFlowNo;

    /** 结算时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结算时间", width = 22)
    private Date settleTime;

    /** 是否需要关注差异（存在待处理差异时回显） */
    private Integer pendingDiffCount;

    /** 用量明细条数 */
    private Integer usageCount;

    /** 复核人ID */
    private Long auditor;

    /** 复核人名称 */
    private String auditorName;

    /** 复核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 创建人ID */
    private Long creator;

    /** 创建人名称 */
    private String creatorName;
}
