package com.ruoyi.crm.enums;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRM 商机状态定义及流转规则
 *
 * <p>状态码以数据字典 {@code crm_bus_status} 为准，并与 crm_business.bus_status 字段一一对应：</p>
 * <pre>
 * 0 初始化  1 跟进中  2 提交测试  3 测试中  4 成交  5 暂停  6 中断
 * </pre>
 *
 * <p>允许的流转路径（白名单，未列出的一律视为非法流转）：</p>
 * <pre>
 * 0 初始化   → 1 跟进中、6 中断
 * 1 跟进中   → 2 提交测试、4 成交、5 暂停、6 中断
 * 2 提交测试 → 3 测试中、5 暂停、6 中断
 * 3 测试中   → 4 成交、5 暂停、6 中断
 * 4 成交     → 终态，不可再流转
 * 5 暂停     → 1 跟进中、2 提交测试、3 测试中、6 中断
 * 6 中断     → 1 跟进中（重新激活）
 * </pre>
 *
 * @author ruoyi
 * @date 2026-10-28
 */
public enum BusStatusEnum
{
    /** 初始化 */
    INIT(0L, "初始化"),

    /** 跟进中 */
    FOLLOWING(1L, "跟进中"),

    /** 提交测试 */
    SUBMIT_TEST(2L, "提交测试"),

    /** 测试中 */
    TESTING(3L, "测试中"),

    /** 成交 */
    DEAL(4L, "成交"),

    /** 暂停 */
    PAUSED(5L, "暂停"),

    /** 中断 */
    INTERRUPTED(6L, "中断");

    /** 状态码，对应 bus_status 字段 */
    private final Long code;

    /** 状态名称 */
    private final String info;

    BusStatusEnum(Long code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public Long getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }

    /** 状态流转白名单：key 为当前状态码，value 为允许流转到的目标状态 */
    private static final Map<Long, Set<BusStatusEnum>> TRANSITIONS;

    static
    {
        Map<Long, Set<BusStatusEnum>> map = new HashMap<>(16);
        map.put(INIT.getCode(), EnumSet.of(FOLLOWING, INTERRUPTED));
        map.put(FOLLOWING.getCode(), EnumSet.of(SUBMIT_TEST, DEAL, PAUSED, INTERRUPTED));
        map.put(SUBMIT_TEST.getCode(), EnumSet.of(TESTING, PAUSED, INTERRUPTED));
        map.put(TESTING.getCode(), EnumSet.of(DEAL, PAUSED, INTERRUPTED));
        map.put(DEAL.getCode(), EnumSet.noneOf(BusStatusEnum.class));
        map.put(PAUSED.getCode(), EnumSet.of(FOLLOWING, SUBMIT_TEST, TESTING, INTERRUPTED));
        map.put(INTERRUPTED.getCode(), EnumSet.of(FOLLOWING));
        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * 根据状态码解析枚举
     *
     * @param code 状态码
     * @return 匹配的枚举，未匹配到返回 null
     */
    public static BusStatusEnum of(Long code)
    {
        if (code == null)
        {
            return null;
        }
        for (BusStatusEnum status : values())
        {
            if (status.code.equals(code))
            {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断当前状态是否允许流转到目标状态
     *
     * @param target 目标状态
     * @return 允许返回 true
     */
    public boolean canTransitTo(BusStatusEnum target)
    {
        if (target == null || target == this)
        {
            return false;
        }
        Set<BusStatusEnum> allowed = TRANSITIONS.get(this.code);
        return allowed != null && allowed.contains(target);
    }

    /**
     * 获取当前状态允许流转到的目标状态列表
     *
     * @return 目标状态列表，终态返回空集合
     */
    public List<BusStatusEnum> allowedNextStatuses()
    {
        Set<BusStatusEnum> allowed = TRANSITIONS.get(this.code);
        if (allowed == null || allowed.isEmpty())
        {
            return Collections.emptyList();
        }
        return allowed.stream()
                .sorted(Comparator.comparing(BusStatusEnum::getCode))
                .collect(Collectors.toList());
    }

    /**
     * 是否为终态（无可流转目标）
     */
    public boolean isFinal()
    {
        return allowedNextStatuses().isEmpty();
    }

    /**
     * 是否允许删除
     *
     * <p>仅【初始化】【跟进中】可删除；进入【提交测试】及之后的状态已启动测试与成交流程，
     * 删除会丢失过程数据，因此一律禁止。</p>
     *
     * @return 允许删除返回 true
     */
    public boolean isDeletable()
    {
        return this == INIT || this == FOLLOWING;
    }
}
