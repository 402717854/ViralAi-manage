package com.ruoyi.crm.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * 差异处理请求对象
 *
 * <p>调账时按「应收调整额 / 成本调整额」生成一条冲销用量，默认按负数冲减；
 * 两个金额均未填时，按原用量金额全额冲销。豁免与驳回不需要金额。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBillDiffHandleVo
{
    /** 差异ID */
    private Long diffId;

    /** 处理方式：1 调账 / 2 豁免 / 3 驳回 */
    private Integer handleStatus;

    /** 处理意见 */
    private String handleRemark;

    /** 应收调整额（负数冲减，正数补收；为空时按原用量金额冲销） */
    private BigDecimal adjustSellAmount;

    /** 成本调整额（负数冲减，正数补收；为空时按原用量金额冲销） */
    private BigDecimal adjustCostAmount;
}
