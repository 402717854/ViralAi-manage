package com.ruoyi.crm.enums;

/**
 * 对账账单状态枚举
 *
 * <p>流转：生成账单 → 待复核(1) → 复核确认(2) → 已结算(3)；
 * 复核时发现未处理差异可标记「差异挂起(4)」，差异处理完毕后重新复核。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum ContractBillStatusEnum
{
    /** 待生成（预留，正常不会出现） */
    PENDING(0, "待生成"),

    /** 待复核：账单已生成，等待财务复核 */
    WAIT_AUDIT(1, "待复核"),

    /** 已确认：复核通过，可作为结算依据 */
    CONFIRMED(2, "已确认"),

    /** 已结算：结算完成（资金扣减 / 线下收款） */
    SETTLED(3, "已结算"),

    /** 差异挂起：存在未处理差异，暂缓结算 */
    DIFF_HANG(4, "差异挂起");

    private final Integer code;

    private final String name;

    ContractBillStatusEnum(Integer code, String name)
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
    public static ContractBillStatusEnum getByCode(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (ContractBillStatusEnum item : values())
        {
            if (item.code.equals(code))
            {
                return item;
            }
        }
        return null;
    }
}
