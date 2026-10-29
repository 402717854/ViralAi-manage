package com.ruoyi.crm.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM 客户对象 crm_customer
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
@Getter
@Setter
public class CrmCustomer extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号，主键自增 */
    private Long id;

    /** 客户名称 */
    private String name;

    /** 跟进状态 */
    private Integer followUpStatus;

    /** 最后跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contactLastTime;

    /** 最后跟进内容 */
    private String contactLastContent;

    /** 下次联系时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contactNextTime;

    /** 负责人 */
    private String ownerUser;

    /** 成为负责人的时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date ownerTime;

    /** 锁定状态 */
    private Integer lockStatus;

    /** 客户状态 */
    private Integer status;

    /** 手机 */
    private String mobile;

    /** 电话 */
    private String telephone;

    /** QQ */
    private String qq;

    /** 微信 */
    private String wechat;

    /** 邮箱 */
    private String email;

    /** 地区编号 */
    private Long areaId;

    /** 详细地址 */
    private String detailAddress;

    /** 所属行业 */
    private Long industryId;

    private String industry;

    /** 客户等级 */
    private Long level;

    /** 客户来源 */
    private Long source;

    /** 创建者 */
    private Long creator;

    /** 更新者 */
    private Long updater;

    /** 是否删除 */
    private Integer deleted;

    /** 租户编号 */
    private Long tenantId;

}
