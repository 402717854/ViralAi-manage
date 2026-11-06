package com.ruoyi.crm.enums;

/**
 * 用量匹配状态枚举
 *
 * <p>与 {@code crm_contract_usage.match_status} 一一对应，同时驱动差异类型映射：
 * 匹配失败即生成 {@code crm_contract_bill_diff} 记录。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum UsageMatchStatusEnum
{
    /** 待匹配：流水已入库但尚未执行匹配 */
    PENDING(0, "待匹配"),

    /** 匹配成功：已找到生效合同明细并完成计费 */
    SUCCESS(1, "匹配成功"),

    /** 无生效合同：客户+渠道不存在任何已签约合同 */
    NO_VALID_CONTRACT(2, "无生效合同"),

    /** 合同已过期：存在已签约合同，但调用时间不在有效期内 */
    CONTRACT_EXPIRED(3, "合同已过期"),

    /** 模型不在合同清单内 */
    PRODUCT_NOT_IN_CONTRACT(4, "模型不在该合同清单内"),

    /** 渠道不匹配：合同明细渠道与流水渠道不一致 */
    SUPPLIER_MISMATCH(5, "渠道不匹配"),

    /** 其他异常：模型未维护、价格缺失、账户不一致等 */
    OTHER_ERROR(6, "其他异常"),

    /** 合同冲突：同一客户+渠道+账户+模型在同一时间存在多份生效合同明细（脏数据，需人工处理） */
    CONTRACT_CONFLICT(7, "多份生效合同");

    private final Integer code;

    private final String name;

    UsageMatchStatusEnum(Integer code, String name)
    {
        this.code = code;
        this.name = name;
    }

    public Integer getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 编码
     * @return 枚举，未匹配返回 null
     */
    public static UsageMatchStatusEnum getByCode(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (UsageMatchStatusEnum item : values())
        {
            if (item.code.equals(code))
            {
                return item;
            }
        }
        return null;
    }

    /**
     * 是否匹配成功
     *
     * @param code 编码
     * @return true 成功
     */
    public static boolean isSuccess(Integer code)
    {
        return SUCCESS.getCode().equals(code);
    }
}
