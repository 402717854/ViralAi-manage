package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 子账单传输对象
 *
 * <p>查询入参：billId / billNo / subBillNo / billCycle / contractId / contactNo /
 * supplierId / customerId，另支持 {@code params.beginDate}、{@code params.endDate}
 * （账单日期范围，yyyy-MM-dd）与 {@code params.scopeSupplierIds}（渠道数据权限）。</p>
 *
 * <p>展示字段中的合同编号、客户名称、渠道名称、父账单状态由 SQL join 得出，
 * 供「子账单明细」二级页面直接渲染，避免前端二次请求。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmContractBillSubDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 子账单编号 */
    @Excel(name = "子账单编号", width = 28)
    private String subBillNo;

    /** 父账单ID */
    private Long billId;

    /** 父账单编号 */
    @Excel(name = "父账单编号", width = 26)
    private String billNo;

    /** 账期 */
    @Excel(name = "账期")
    private String billCycle;

    /** 账单日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "账单日期", width = 14)
    private Date billDate;

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

    /** 渠道账户ID，0 表示未归属 */
    private Long supplierAccountId;

    /** 归集用量条数 */
    private Integer usageCount;

    /** 当日总token */
    @Excel(name = "总token")
    private Long totalTokens;

    /** 当日应收金额 */
    @Excel(name = "应收总额", scale = 8)
    private BigDecimal totalSellAmount;

    /** 当日成本 */
    @Excel(name = "总成本", scale = 8)
    private BigDecimal totalCostAmount;

    /** 当日毛利 */
    @Excel(name = "总毛利", scale = 8)
    private BigDecimal totalGrossProfit;

    /** 父账单状态（子账单不独立维护状态） */
    private Integer billStatus;
}
