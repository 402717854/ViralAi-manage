package com.ruoyi.crm.enums;

/**
 * 对账差异类型枚举
 *
 * <p>由匹配状态映射而来：无合同(2) → 无合同，合同过期(3) → 合同过期，
 * 其他异常(6) 归为价格异常，「模型不在合同/渠道不匹配」归为其他。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum BillDiffTypeEnum
{
    /** 无合同 */
    NO_CONTRACT(1, "无合同"),

    /** 合同过期 */
    CONTRACT_EXPIRED(2, "合同过期"),

    /** 价格异常：基准价或折扣缺失 */
    PRICE_ERROR(3, "价格异常"),

    /** token 不一致 */
    TOKEN_MISMATCH(4, "token不一致"),

    /** 其他 */
    OTHER(5, "其他");

    private final Integer code;

    private final String name;

    BillDiffTypeEnum(Integer code, String name)
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
     * 由匹配状态推导差异类型
     *
     * @param matchStatus 匹配状态
     * @return 差异类型，匹配成功或待匹配返回 null
     */
    public static BillDiffTypeEnum fromMatchStatus(Integer matchStatus)
    {
        if (matchStatus == null)
        {
            return null;
        }
        if (UsageMatchStatusEnum.NO_VALID_CONTRACT.getCode().equals(matchStatus))
        {
            return NO_CONTRACT;
        }
        if (UsageMatchStatusEnum.CONTRACT_EXPIRED.getCode().equals(matchStatus))
        {
            return CONTRACT_EXPIRED;
        }
        if (UsageMatchStatusEnum.OTHER_ERROR.getCode().equals(matchStatus))
        {
            return PRICE_ERROR;
        }
        if (UsageMatchStatusEnum.PRODUCT_NOT_IN_CONTRACT.getCode().equals(matchStatus)
                || UsageMatchStatusEnum.SUPPLIER_MISMATCH.getCode().equals(matchStatus)
                || UsageMatchStatusEnum.CONTRACT_CONFLICT.getCode().equals(matchStatus))
        {
            return OTHER;
        }
        return null;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 编码
     * @return 枚举，未匹配返回 null
     */
    public static BillDiffTypeEnum getByCode(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (BillDiffTypeEnum item : values())
        {
            if (item.code.equals(code))
            {
                return item;
            }
        }
        return null;
    }
}
