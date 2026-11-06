package com.ruoyi.crm.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

/**
 * 商机关联合同（详情页「关联合同」Tab 展示，仅保留跳转与识别所需字段）
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessContractVo
{
    /** 合同ID */
    private Long id;

    /** 合同编号 */
    private String contactNo;

    /** 合同名称 */
    private String contractName;

    /** 合同状态：0草稿 1审批中 2已签约 3已到期 4已作废 5已归档 */
    private Integer status;

    /** 合同状态名称 */
    private String statusName;

    /** 签约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signTime;

    /** 合同到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
