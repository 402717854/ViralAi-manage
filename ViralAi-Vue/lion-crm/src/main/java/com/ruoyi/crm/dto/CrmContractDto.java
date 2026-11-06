package com.ruoyi.crm.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.crm.domain.CrmContractLog;

/**
 * 合同列表与详情传输对象
 *
 * <p>列表查询入参：contactNo / contractName / supplierId / consumerId / status，
 * 另支持 params.beginTime、params.endTime（创建时间范围）与 params.statusList（逗号分隔多选状态）。
 * 详情返回：主信息 + products 明细 + logs 操作日志 + auditRecords 审批记录。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商机ID */
    private Long busId;

    /** 商机名称 */
    private String busName;

    /** 客户ID */
    private Long consumerId;

    /** 客户名称 */
    private String consumerName;

    /** 渠道ID：仅作为列表筛选条件，按明细行渠道匹配 */
    private Long supplierId;

    /** 涉及渠道名称：按明细行聚合，多个渠道以「、」分隔 */
    private String supplierName;

    /** 渠道编码：合同不再绑定单一渠道，该字段已废弃，恒为空 */
    private String supplierCode;

    /** 合同编号 */
    private String contactNo;

    /** 合同名称 */
    private String contractName;

    /** 合同状态：0草稿 1审批中 2已签约 3已到期 4已作废 5已归档 */
    private Integer status;

    /** 合同状态名称 */
    private String statusName;

    /** 签约主体 */
    private String contractEntity;

    /** 合同到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 签约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    /** 作废时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 归档时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date archiveTime;

    /** 审批意见 */
    private String auditOpinion;

    /** 作废原因 */
    private String cancelReason;

    /** 创建人ID */
    private Long creatorId;

    /** 创建人名称 */
    private String creatorName;

    /** 更新人名称 */
    private String updaterName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 是否删除 */
    private Integer deleted;

    /** 模型产品明细（详情返回） */
    private List<CrmContractProductDto> products;

    /** 操作日志（详情返回） */
    private List<CrmContractLog> logs;

    /** 审批记录（详情返回，action 为审批通过/驳回的日志） */
    private List<CrmContractLog> auditRecords;
}
