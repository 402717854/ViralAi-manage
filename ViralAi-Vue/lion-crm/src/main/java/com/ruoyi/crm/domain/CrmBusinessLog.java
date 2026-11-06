package com.ruoyi.crm.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商机操作日志对象 crm_business_log
 *
 * <p>状态流转、产品清单维护、转合同、结算回填均写入该表，供详情页「操作日志」Tab 展示。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 商机ID */
    private Long busId;

    /** 操作类型 */
    private String action;

    /** 操作名称 */
    private String actionName;

    /** 变更前状态 */
    private Integer fromStatus;

    /** 变更后状态 */
    private Integer toStatus;

    /** 操作内容 */
    private String content;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
