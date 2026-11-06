package com.ruoyi.crm.dto;

import java.util.List;

import com.ruoyi.crm.domain.CrmCustomer;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户保存对象
 * <p>
 * 在客户主表字段之外携带关联渠道ID集合：
 * <ul>
 *   <li>null：本次不涉及渠道变更（兼容只改客户资料的调用方）；</li>
 *   <li>空集合：清空该客户的全部渠道关联；</li>
 *   <li>非空：全量替换为该集合。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmCustomerVo extends CrmCustomer
{
    private static final long serialVersionUID = 1L;

    /** 关联渠道ID集合 */
    private List<Long> supplierIds;
}
