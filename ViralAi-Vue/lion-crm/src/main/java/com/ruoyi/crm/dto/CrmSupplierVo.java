package com.ruoyi.crm.dto;

import java.util.List;

import com.ruoyi.crm.domain.CrmSupplierProject;
import lombok.Getter;
import lombok.Setter;

/**
 * CRM 供应商请求/详情对象
 * <p>
 * 在供应商主表字段基础上增加 projects 列表，用于一次提交供应商及其产品关联信息。
 * 一对多关系通过 projects 承载，列表元素的 projectId 对应 crm_model_product.id。
 *
 * @author ruoyi
 * @date 2026-10-29
 */
@Getter
@Setter
public class CrmSupplierVo
{
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

    /** 备注 */
    private String remark;

    /**
     * 供应商产品关联列表（一对多）
     * <p>
     * 语义约定：为 null 表示本次不涉及关联变更；为空数组表示清空全部关联；非空表示按提交内容全量替换。
     * 列表内 projectId 重复时静默去重，仅保留第一条。
     */
    private List<CrmSupplierProject> projects;
}
