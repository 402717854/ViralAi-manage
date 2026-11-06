package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 商机保存入参
 *
 * <p>id 为空表示新建（状态强制【初始化】）；id 非空表示编辑，仅【初始化】【跟进中】允许保存。
 * 商机只绑定客户，不再接收渠道入参：客户 1:N 渠道，具体渠道由明细行的 supplierId 决定。
 * products 为 null 时不涉及产品清单变更；非 null 时全量替换，并执行「客户已开通渠道」归属强校验。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessVo
{
    /** 商机ID */
    private Long id;

    /** 商机名称 */
    private String name;

    /** 客户ID */
    private Long customerId;

    /** 负责人 */
    private String ownerUser;

    /** 意向合同金额（前端按明细汇总，允许手动微调） */
    private BigDecimal contactAmount;

    /** 意向授信额度 */
    private BigDecimal creditAmount;

    /** 备注 */
    private String remark;

    /** 意向产品清单，null 表示不变更 */
    private List<CrmBusinessProductDto> products;
}
