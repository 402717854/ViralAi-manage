package com.ruoyi.crm.domain;

import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM 供应商对象 crm_supplier
 * 
 * @author ruoyi
 * @date 2026-10-27
 */
@Getter
@Setter
public class CrmSupplier extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号，主键自增 */
    private Long id;

    /** 供应商名称 */
    private String name;

    /** 负责人 */
    private String ownerUser;

    /** 状态 */
    private Integer status;

    /** 手机 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 详细地址 */
    private String detailAddress;

    /** 结算方式 */
    private Long payType;

    /** 创建者 */
    private Long creator;

    /** 更新者 */
    private Long updater;

    /** 是否删除 */
    private Integer deleted;

}
