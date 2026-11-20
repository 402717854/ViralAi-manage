package com.ruoyi.crm.enums;

/**
 * 客户资金流水业务类型
 *
 * <p>方向由业务类型唯一决定，调用方不需要（也不允许）单独传方向，
 * 避免出现「充值却记成出账」这类致命错账。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public enum CustomerFundBizTypeEnum
{
    /** 充值入账 */
    RECHARGE(1, "充值", 1),

    /** 账单结算扣减 */
    SETTLE_DEDUCT(2, "结算扣款", 2),

    /** 退款 */
    REFUND(3, "退款", 2),

    /** 人工调增 */
    ADJUST_ADD(4, "人工调增", 1),

    /** 人工调减 */
    ADJUST_SUB(5, "人工调减", 2),

    /** 冲销退回：负数账单（差异冲销）结算时把钱退回客户 */
    REVERSE(6, "冲销退回", 1),

    /** 期初余额：上线切换时导入的历史在账余额 */
    INIT(7, "期初余额", 1),

    /** 结算撤销：撤销已结算账单，红冲原扣款流水 */
    SETTLE_CANCEL(8, "结算撤销", 1),

    /**
     * 撤销冲销：撤销「负数账单退回」时把钱扣回。
     *
     * <p>方向必须与 {@link #SETTLE_CANCEL} 相反：负数账单结算时是入账，
     * 撤销该结算自然要出账，否则两次操作都加钱，账户凭空多出金额。</p>
     */
    SETTLE_CANCEL_BACK(9, "撤销冲销", 2);

    /** 入账方向 */
    public static final int DIRECTION_IN = 1;

    /** 出账方向 */
    public static final int DIRECTION_OUT = 2;

    private final Integer code;

    private final String name;

    private final Integer direction;

    CustomerFundBizTypeEnum(Integer code, String name, Integer direction)
    {
        this.code = code;
        this.name = name;
        this.direction = direction;
    }

    public Integer getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }

    public Integer getDirection()
    {
        return direction;
    }

    /**
     * 是否入账方向
     *
     * @return true 入账
     */
    public boolean isIncome()
    {
        return DIRECTION_IN == direction;
    }

    /**
     * 按编码获取枚举
     *
     * @param code 编码
     * @return 枚举，未匹配返回 null
     */
    public static CustomerFundBizTypeEnum getByCode(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (CustomerFundBizTypeEnum item : values())
        {
            if (item.code.equals(code))
            {
                return item;
            }
        }
        return null;
    }
}
