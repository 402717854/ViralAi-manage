package com.ruoyi.crm.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class CrmBusiness extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号 */
    private Long id;

    /** 商机名称 */
    private String name;

    /** 客户编号 */
    private Long customerId;

    /** 渠道编号 */
    private Long supplierId;

    /** 负责人 */
    private String ownerUser;

    /** 商机状态0初始化1跟进中2提交测试3测试中4成交5暂停6中断（字典 crm_bus_status） */
    private Long busStatus;

    /** 成交日期 */
    private Date dealTime;

    /** 合同状态0未签1已签 */
    private Integer contactStatus;

    /** 结算状态0未结算1已结算 */
    private Integer settleStatus;

    /** 合同金额 */
    private BigDecimal contactAmount;

    /** 授信金额 */
    private BigDecimal creditAmount;

    /** 中断备注 */
    private String failRemark;

    /** 创建人 */
    private Long creator;

    /** 更新人 */
    private Long updater;

    /** 逻辑删除 */
    private Integer deleted;

}
