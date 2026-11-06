package com.ruoyi.crm.enums;

/**
 * 合同操作日志类型
 *
 * <p>对应 crm_contract_log.action 字段，操作名称写库时冗余到 action_name，
 * 便于前端直接展示且不受后续枚举调整影响。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum ContractActionEnum
{
    /** 新建合同 */
    CREATE("CREATE", "新建合同"),

    /** 编辑合同 */
    UPDATE("UPDATE", "编辑合同"),

    /** 维护模型明细 */
    PRODUCT_SAVE("PRODUCT_SAVE", "维护模型明细"),

    /** 提交审批 */
    SUBMIT("SUBMIT", "提交审批"),

    /** 撤回审批 */
    WITHDRAW("WITHDRAW", "撤回审批"),

    /** 审批通过 */
    AUDIT_PASS("AUDIT_PASS", "审批通过"),

    /** 审批驳回 */
    AUDIT_REJECT("AUDIT_REJECT", "审批驳回"),

    /** 合同作废 */
    CANCEL("CANCEL", "合同作废"),

    /** 合同归档 */
    ARCHIVE("ARCHIVE", "合同归档"),

    /** 删除草稿 */
    DELETE("DELETE", "删除草稿"),

    /** 到期自动变更 */
    EXPIRE("EXPIRE", "合同到期");

    /** 操作编码 */
    private final String code;

    /** 操作名称 */
    private final String name;

    ContractActionEnum(String code, String name)
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
