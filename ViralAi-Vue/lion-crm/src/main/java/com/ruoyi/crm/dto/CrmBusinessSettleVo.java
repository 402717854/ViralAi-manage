package com.ruoyi.crm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 商机结算状态回填请求对象
 *
 * <p>结算状态独立于合同签约状态，由运营/管理人员在对账完成后人工回填。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessSettleVo
{
    /** 商机ID */
    @NotNull(message = "商机编号不能为空")
    private Long id;

    /** 结算状态：0未结算 1已结算 */
    @NotNull(message = "结算状态不能为空")
    @Min(value = 0, message = "结算状态只能为 0 或 1")
    @Max(value = 1, message = "结算状态只能为 0 或 1")
    private Integer settleStatus;
}
