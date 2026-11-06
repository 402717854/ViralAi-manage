package com.ruoyi.crm.domain;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户渠道关联对象 crm_customer_supplier
 * <p>
 * 客户 1 : N 渠道。商机与合同不再直接绑定渠道，而是通过客户关联的渠道决定
 * 可选的模型范围；具体某一行明细使用哪个渠道，由明细行的 supplier_id 记录。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmCustomerSupplier
{
    /** 主键 */
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 渠道ID */
    private Long supplierId;

    /** 创建者 */
    private Long creator;

    /** 创建时间 */
    private Date createTime;
}
