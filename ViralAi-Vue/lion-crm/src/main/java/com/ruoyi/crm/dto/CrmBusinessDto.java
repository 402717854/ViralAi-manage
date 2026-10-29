package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.crm.domain.CrmBusinessProduct;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM 商机对象 crm_business
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmBusinessDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号 */
    private Long id;

    /** 商机名称 */
    @Excel(name = "商机名称")
    private String name;

    /** 客户编号 */
    @Excel(name = "客户")
    private String customer;
    private Long customerId;

    /** 渠道编号 */
    @Excel(name = "渠道")
    private String supplier;
    private Long supplierId;

    /** 负责人 */
    @Excel(name = "负责人")
    private String ownerUser;

    /** 商机状态0初始化1跟进中2提交测试3测试中4成交5暂停6中断（字典 crm_bus_status） */
    @Excel(name = "商机状态", readConverterExp = "0=初始化,1=跟进中,2=提交测试,3=测试中,4=成交,5=暂停,6=中断")
    private Long busStatus;

    /** 成交日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "成交日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date dealTime;

    /** 合同状态0未签1已签 */
    @Excel(name = "合同状态")
    private Integer contactStatus;

    /** 结算状态0未结算1已结算 */
    @Excel(name = "结算状态")
    private Integer settleStatus;

    /** 合同金额 */
    @Excel(name = "合同金额")
    private BigDecimal contactAmount;

    /** 授信金额 */
    @Excel(name = "授信金额")
    private BigDecimal creditAmount;

    /** 中断备注 */
    @Excel(name = "中断备注")
    private String failRemark;

    /** 创建人 */
    @Excel(name = "创建人")
    private String creator;

    /** 更新人 */
    @Excel(name = "更新人")
    private String updater;

    /** 逻辑删除 */
    @Excel(name = "逻辑删除")
    private Integer deleted;


    private List<CrmBusinessProduct> businessProducts;

}
