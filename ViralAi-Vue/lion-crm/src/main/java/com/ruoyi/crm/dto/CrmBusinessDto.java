package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商机列表/导出对象 crm_business
 *
 * <p>列表附带客户名称、渠道名称、操作人名称，以及是否已生成合同、关联合同编号，
 * 供列表页「是否签合同」「查看合同」按钮直接使用，避免逐行二次查询。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商机名称 */
    @Excel(name = "商机名称")
    private String name;

    /** 客户名称 */
    @Excel(name = "客户")
    private String customer;

    /** 客户ID */
    private Long customerId;

    /** 涉及渠道名称（按明细行聚合，多个渠道以「、」分隔） */
    @Excel(name = "渠道")
    private String supplier;

    /** 渠道ID：仅作为列表筛选条件，按明细行渠道匹配 */
    private Long supplierId;

    /** 负责人 */
    @Excel(name = "负责人")
    private String ownerUser;

    /** 商机状态：0初始化 1跟进中 2提交测试 3测试中 4成交 5暂停 6中断 */
    @Excel(name = "商机状态", readConverterExp = "0=初始化,1=跟进中,2=提交测试,3=测试中,4=成交,5=暂停,6=中断")
    private Long busStatus;

    /** 商机状态名称（后端枚举回填，前端可直接展示） */
    private String statusName;

    /** 成交时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "成交时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date dealTime;

    /** 合同状态：0未签 1已签 */
    @Excel(name = "合同状态", readConverterExp = "0=未签,1=已签")
    private Integer contactStatus;

    /** 意向合同金额 */
    @Excel(name = "意向金额")
    private BigDecimal contactAmount;

    /** 意向授信额度 */
    @Excel(name = "授信金额")
    private BigDecimal creditAmount;

    /** 中断备注 */
    @Excel(name = "中断备注")
    private String failRemark;

    /** 创建人名称 */
    @Excel(name = "创建人")
    private String creator;

    /** 更新人名称 */
    @Excel(name = "更新人")
    private String updater;

    /** 是否已生成合同：1是 0否 */
    private Integer hasContract;

    /** 最近一份合同ID，用于列表「查看合同」跳转 */
    private Long contractId;

    /** 最近一份合同编号 */
    private String contractNo;

    /** 产品条数 */
    private Integer productCount;

    /** 逻辑删除：0否 1是 */
    private Integer deleted;

    /** 意向产品明细（详情返回时使用） */
    private List<CrmBusinessProductDto> businessProducts;
}
