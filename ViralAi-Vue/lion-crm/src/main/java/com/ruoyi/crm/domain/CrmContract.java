package com.ruoyi.crm.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 合同对象 crm_contract
 *
 * <p>合同只绑定客户（consumer_id）；客户 1:N 渠道，具体渠道由合同明细行（supplier_id）承载，
 * 因此一份合同可跨多个渠道，各渠道分别出账。状态流转规则见 {@link com.ruoyi.crm.enums.ContractStatusEnum}。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContract extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商机ID */
    private Long busId;

    /** 客户ID */
    private Long consumerId;

    /** 渠道ID：仅用于列表按明细渠道筛选，不作为合同属性持久化（渠道由明细行承载） */
    private Long supplierId;

    /** 合同编号，首次保存时生成，永不修改 */
    private String contactNo;

    /** 合同名称 */
    private String contractName;

    /** 合同状态：0草稿 1审批中 2已签约 3已到期 4已作废 5已归档 */
    private Integer status;

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

    /** 创建者 */
    private Long creator;

    /** 更新者 */
    private Long updater;

    /** 是否删除 0否 1是 */
    private Integer deleted;
}
