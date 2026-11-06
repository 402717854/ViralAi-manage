package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 对账差异传输对象
 *
 * <p>查询入参：billId / usageId / diffType / handleStatus / supplierId / customerId / callNo，
 * 另支持 params.beginTime、params.endTime（调用时间范围）与 params.scopeSupplierIds（数据权限）。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractBillDiffDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账单ID */
    private Long billId;

    /** 账单编号 */
    private String billNo;

    /** 异常用量记录ID */
    private Long usageId;

    /** 差异类型 */
    @Excel(name = "差异类型", readConverterExp = "1=无合同,2=合同过期,3=价格异常,4=token不一致,5=其他")
    private Integer diffType;

    /** 差异描述 */
    @Excel(name = "差异描述", width = 46)
    private String diffDesc;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    @Excel(name = "渠道", width = 20)
    private String supplierName;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    @Excel(name = "客户", width = 20)
    private String customerName;

    /** 网关流水号 */
    @Excel(name = "流水号", width = 34)
    private String callNo;

    /** 调用时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调用时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date callTime;

    /** 涉及金额（应收） */
    @Excel(name = "涉及金额", scale = 8)
    private BigDecimal amount;

    /** 处理状态 */
    @Excel(name = "处理状态", readConverterExp = "0=待处理,1=已调账,2=豁免,3=驳回")
    private Integer handleStatus;

    /** 处理意见 */
    @Excel(name = "处理意见", width = 40)
    private String handleRemark;

    /** 处理人ID */
    private Long handler;

    /** 处理人名称 */
    @Excel(name = "处理人", width = 14)
    private String handlerName;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "处理时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
}
