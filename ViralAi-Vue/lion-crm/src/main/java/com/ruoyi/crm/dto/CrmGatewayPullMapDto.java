package com.ruoyi.crm.dto;

import java.util.Date;

import com.ruoyi.common.annotation.Excel;

import lombok.Getter;
import lombok.Setter;

/**
 * 上游标识映射查询结果对象
 *
 * <p>在映射表字段之外附加渠道名、账户名、客户名与模型名，
 * 让维护人员不必对照主键就能确认映射指向了正确的对象。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Getter
@Setter
public class CrmGatewayPullMapDto
{
    /** 主键 */
    private Long id;

    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    @Excel(name = "渠道")
    private String supplierName;

    /** 渠道账户ID，0 表示渠道通用 */
    private Long supplierAccountId;

    /** 渠道账户名称 */
    @Excel(name = "账户")
    private String accountName;

    /** 映射类型 CUSTOMER / PRODUCT */
    @Excel(name = "映射类型")
    private String mapType;

    /** 上游标识 */
    @Excel(name = "上游标识")
    private String upstreamKey;

    /** 上游变体 */
    @Excel(name = "上游变体")
    private String upstreamVariant;

    /** 目标ID（客户ID） */
    private Long targetId;

    /** 客户名称 */
    @Excel(name = "客户")
    private String customerName;

    /** 目标编码（模型编码） */
    @Excel(name = "模型编码")
    private String targetCode;

    /** 模型厂商 */
    @Excel(name = "模型厂商")
    private String modelName;

    /** 模型型号 */
    @Excel(name = "模型型号")
    private String modelNo;

    /** 状态 0失效 1有效 */
    @Excel(name = "状态")
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建者 */
    private Long creator;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
