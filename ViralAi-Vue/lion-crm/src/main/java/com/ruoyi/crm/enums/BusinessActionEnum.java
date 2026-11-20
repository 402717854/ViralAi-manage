package com.ruoyi.crm.enums;

/**
 * 商机操作日志类型
 *
 * <p>对应 crm_business_log.action 字段，操作名称写库时冗余到 action_name，
 * 便于前端直接展示且不受后续枚举调整影响。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum BusinessActionEnum
{
    /** 新建商机 */
    CREATE("CREATE", "新建商机"),

    /** 编辑商机 */
    UPDATE("UPDATE", "编辑商机"),

    /** 状态变更 */
    STATUS("STATUS", "状态变更"),

    /** 维护产品清单 */
    PRODUCT("PRODUCT", "维护产品清单"),

    /** 转合同 */
    CONVERT("CONVERT", "转合同"),

    /** 合同状态回写（合同签约 / 作废联动） */
    CONTACT("CONTACT", "合同状态回写"),

    /** 删除商机 */
    DELETE("DELETE", "删除商机");

    /** 操作编码 */
    private final String code;

    /** 操作名称 */
    private final String name;

    BusinessActionEnum(String code, String name)
    {
        this.code = code;
        this.name = name;
    }

    public String getCode()
    {
        return code;
    }

    public String getName()
    {
        return name;
    }
}
