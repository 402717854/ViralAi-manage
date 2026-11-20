package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户资金流水传输对象
 *
 * <p>查询入参：customerId / bizType / direction / billNo / params.beginTime / params.endTime。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmCustomerAccountFlowDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 流水号 */
    @Excel(name = "流水号", width = 26)
    private String flowNo;

    /** 账户ID */
    private Long accountId;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    @Excel(name = "客户", width = 24)
    private String customerName;

    /** 业务类型 */
    private Integer bizType;

    /** 业务类型名称 */
    @Excel(name = "类型", width = 14)
    private String bizTypeName;

    /** 业务单据ID */
    private Long bizId;

    /** 业务单据编号 */
    @Excel(name = "关联单号", width = 26)
    private String bizNo;

    /** 方向：1入账 2出账 */
    private Integer direction;

    /** 变动金额（恒为正） */
    @Excel(name = "金额", scale = 2)
    private BigDecimal amount;

    /** 变动前余额 */
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    @Excel(name = "变动后余额", scale = 2)
    private BigDecimal balanceAfter;

    /** 关联账单ID */
    private Long billId;

    /** 关联账单编号 */
    private String billNo;

    /** 关联合同ID */
    private Long contractId;

    /** 操作人 */
    private Long operator;

    /** 操作人姓名 */
    @Excel(name = "操作人", width = 14)
    private String operatorName;

    /** 流水生成时间（对应 create_time，避免与 BaseEntity.createTime 字段遮蔽，命名区分） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "时间", width = 22)
    private Date flowTime;

    /** 查询条件：账期起（yyyy-MM-dd HH:mm:ss），与 params.beginTime 二选一 */
    private String beginTimeStr;

    /** 查询条件：账期止 */
    private String endTimeStr;
}
