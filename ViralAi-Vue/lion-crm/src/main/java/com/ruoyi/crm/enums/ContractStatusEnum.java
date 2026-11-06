package com.ruoyi.crm.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 合同状态定义及流转规则
 *
 * <p>状态码与 crm_contract.status 一一对应，并与数据字典 {@code crm_contract_status} 保持一致：</p>
 * <pre>
 * 0 草稿  1 审批中  2 已签约  3 已到期  4 已作废  5 已归档
 * </pre>
 *
 * <p>允许的流转路径（白名单，未列出的一律视为非法流转）：</p>
 * <pre>
 * 0 草稿   → 1 审批中（提交审批）
 * 1 审批中 → 0 草稿（审批驳回 / 撤回）、2 已签约（审批通过）
 * 2 已签约 → 3 已到期（定时任务）、4 已作废、5 已归档
 * 3 已到期 → 5 已归档
 * 4 已作废 → 5 已归档
 * 5 已归档 → 终态，不可流转
 * </pre>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public enum ContractStatusEnum
{
    /** 草稿 */
    DRAFT(0, "草稿", "info"),

    /** 审批中 */
    AUDITING(1, "审批中", "primary"),

    /** 已签约 */
    SIGNED(2, "已签约", "success"),

    /** 已到期 */
    EXPIRED(3, "已到期", "warning"),

    /** 已作废 */
    CANCELLED(4, "已作废", "danger"),

    /** 已归档 */
    ARCHIVED(5, "已归档", "info");

    /** 状态码 */
    private final Integer code;

    /** 状态名称 */
    private final String info;

    /** 前端标签样式（el-tag type） */
    private final String tagType;

    ContractStatusEnum(Integer code, String info, String tagType)
    {
        this.code = code;
        this.info = info;
        this.tagType = tagType;
    }

    public Integer getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }

    public String getTagType()
    {
        return tagType;
    }

    /** 状态流转白名单 */
    private static final Map<Integer, Set<ContractStatusEnum>> TRANSITIONS = new HashMap<>();

    static
    {
        TRANSITIONS.put(DRAFT.code, EnumSet.of(AUDITING));
        TRANSITIONS.put(AUDITING.code, EnumSet.of(DRAFT, SIGNED));
        TRANSITIONS.put(SIGNED.code, EnumSet.of(EXPIRED, CANCELLED, ARCHIVED));
        TRANSITIONS.put(EXPIRED.code, EnumSet.of(ARCHIVED));
        TRANSITIONS.put(CANCELLED.code, EnumSet.of(ARCHIVED));
        TRANSITIONS.put(ARCHIVED.code, EnumSet.noneOf(ContractStatusEnum.class));
    }

    /**
     * 按状态码解析枚举
     *
     * @param code 状态码
     * @return 匹配的枚举，未匹配返回 null
     */
    public static ContractStatusEnum of(Integer code)
    {
        if (code == null)
        {
            return null;
        }
        for (ContractStatusEnum status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        return null;
    }

    /**
     * 是否允许流转到目标状态
     *
     * @param target 目标状态
     * @return true 允许流转
     */
    public boolean canTransitTo(ContractStatusEnum target)
    {
        if (target == null)
        {
            return false;
        }
        Set<ContractStatusEnum> allowed = TRANSITIONS.get(this.code);
        return allowed != null && allowed.contains(target);
    }

    /** 是否允许编辑主信息与明细：仅草稿 */
    public boolean isEditable()
    {
        return this == DRAFT;
    }

    /** 是否允许删除：仅草稿 */
    public boolean isDeletable()
    {
        return this == DRAFT;
    }

    /** 是否允许提交审批：仅草稿 */
    public boolean isSubmittable()
    {
        return this == DRAFT;
    }

    /** 是否允许作废：已签约、已到期 */
    public boolean isCancellable()
    {
        return this == SIGNED || this == EXPIRED;
    }

    /** 是否允许归档：已签约、已到期、已作废 */
    public boolean isArchivable()
    {
        return this == SIGNED || this == EXPIRED || this == CANCELLED;
    }

    /** 是否为终态：已归档 */
    public boolean isFinal()
    {
        return this == ARCHIVED;
    }

    public static Integer getCode(ContractStatusEnum status)
    {
        return status == null ? null : status.getCode();
    }

    public static String getInfo(ContractStatusEnum status)
    {
        return status == null ? null : status.getInfo();
    }
}
