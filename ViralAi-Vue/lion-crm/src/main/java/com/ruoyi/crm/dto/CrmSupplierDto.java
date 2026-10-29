package com.ruoyi.crm.dto;

import java.util.List;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.crm.domain.CrmSupplierProject;
import lombok.Getter;
import lombok.Setter;

/**
 * CRM 供应商对象 crm_supplier
 * 
 * @author ruoyi
 * @date 2026-10-27
 */
@Setter
@Getter
public class CrmSupplierDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 编号，主键自增 */
    private Long id;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String name;

    /** 负责人 */
    @Excel(name = "负责人")
    private String ownerUser;

    /** 状态 */
    @Excel(name = "状态")
    private Integer status;

    /** 手机 */
    @Excel(name = "手机")
    private String mobile;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 详细地址 */
    @Excel(name = "详细地址")
    private String detailAddress;

    /** 结算方式 */
    @Excel(name = "结算方式")
    private Long payType;

    /** 创建者 */
    @Excel(name = "创建者")
    private String creator;

    /** 更新者 */
    @Excel(name = "更新者")
    private String updater;

    /** 是否删除 */
    @Excel(name = "是否删除")
    private Integer deleted;

    /** 供应商产品关联列表（一对多），仅详情接口返回，无 @Excel 故不参与导出 */
    private List<CrmSupplierProject> projects;

}
