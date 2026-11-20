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

    /** 渠道类型：1 自营，2 三方；新增时为空由数据库默认值（1 自营）兜底 */
    private Integer supplierType;

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
     * 每行通过 projectSupplierId 承载「模型所属供应商」：为空时落当前供应商；
     * 填其它供应商时表示该模型取自对方的模型池，折扣仍按本供应商口径独立定义，
     * 因此同一供应商下可以挂多个供应商的相同模型。
     * 同一「模型所属供应商 + 模型」重复提交时静默去重，仅保留第一条。
     */
    private List<CrmSupplierProject> projects;
}
