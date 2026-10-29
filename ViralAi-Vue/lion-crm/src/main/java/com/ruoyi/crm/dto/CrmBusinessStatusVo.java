package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * CRM 商机状态流转请求对象
 *
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmBusinessStatusVo
{
    /** 商机编号 */
    @NotNull(message = "商机编号不能为空")
    private Long id;

    /** 目标商机状态 0初始化1跟进中2提交测试3测试中4成交5暂停6中断 */
    @NotNull(message = "目标商机状态不能为空")
    private Long busStatus;

    /** 中断备注，流转到 6 中断 时必填（对应 fail_remark varchar(255)） */
    @Size(max = 255, message = "中断备注长度不能超过255个字符")
    private String failRemark;

    /** 成交日期，仅在流转到 4 成交 时生效，为空则取当前时间 */
    private Date dealTime;

    /**
     * 授信金额，流转到 2 提交测试 时必填且必须大于 0（对应 credit_amount decimal）
     *
     * <p>授信金额是进入测试流程的前置条件，未填写不允许提交测试。</p>
     */
    private BigDecimal creditAmount;
}
