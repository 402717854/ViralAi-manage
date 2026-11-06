package com.ruoyi.crm.enums;

/**
 * 差异处理状态枚举
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum BillDiffHandleStatusEnum
{
    /** 待处理 */
    PENDING(0, "待处理"),

    /** 已调账：生成反向冲销用量，参与下一账期账单 */
    ADJUSTED(1, "已调账"),

    /** 豁免：不计费、不追责 */
    EXEMPTED(2, "豁免"),

    /** 驳回：退回继续核查，仍为待处理状态 */
    REJECTED(3, "驳回");

    private final Integer code;

    private final String name;

    BillDiffHandleStatusEnum(Integer code, String name)
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
    public static BillDiffHandleStatusEnum getByCode(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (BillDiffHandleStatusEnum item : values())
        {
            if (item.code.equals(code))
            {
                return item;
            }
        }
        return null;
    }
}
