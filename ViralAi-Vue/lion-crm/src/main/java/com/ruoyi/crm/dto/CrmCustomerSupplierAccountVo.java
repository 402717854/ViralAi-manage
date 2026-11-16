package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 客户可用的「渠道 + 渠道账户」组合
 *
 * <p>数据来源是客户合同明细行（{@code crm_contract_product}）：合同绑定客户，
 * 明细行绑定渠道与渠道账户。因此这三者的联动关系本身就是业务约束，
 * 上传流水时只允许选择客户合同上真实存在的渠道账户组合。</p>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
@Getter
@Setter
public class CrmCustomerSupplierAccountVo
{
    /** 渠道ID */
    private Long supplierId;

    /** 渠道名称 */
    private String supplierName;

    /** 渠道账户ID */
    private Long supplierAccountId;

    /** 渠道账户名称 */
    private String supplierAccountName;

    /** 账户状态 0未启用 1启用，停用账户仍可能出现（历史合同），仅用于前端标注 */
    private Integer accountStatus;

    /** 流水拉取方式 1路由获取 2文件上传，用于前端标注哪些账户适合人工上传 */
    private Integer pullType;
}
