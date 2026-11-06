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
 * <p>流转合法性由后端 {@link com.ruoyi.crm.enums.BusStatusEnum} 白名单二次校验，前端按钮仅作体验层控制。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessStatusVo
{
    /** 商机ID */
    @NotNull(message = "商机编号不能为空")
    private Long id;

    /** 目标商机状态 0初始化1跟进中2提交测试3测试中4成交5暂停6中断 */
    @NotNull(message = "目标商机状态不能为空")
    private Long busStatus;

    /** 中断备注，流转到 6 中断 时必填（对应 fail_remark varchar(255)） */
    @Size(max = 255, message = "中断备注长度不能超过255个字符")
    private String failRemark;

    /** 成交时间，仅在流转到 4 成交 时生效，为空则取当前时间 */
    private Date dealTime;

    /**
     * 意向授信额度，可选；填写时随流转一并更新（对应 credit_amount decimal）
     *
     * <p>授信额度在商机编辑页维护，流转接口允许同步补录，避免二次修改。</p>
     */
    private BigDecimal creditAmount;
}
