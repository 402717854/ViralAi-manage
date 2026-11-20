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
 * 0 初始化   → 1 跟进中
 * 1 跟进中   → 2 提交测试、5 暂停、6 中断
 * 2 提交测试 → 3 测试中、1 退回跟进、6 中断
 * 3 测试中   → 4 成交、1 退回跟进、5 暂停、6 中断
 * 4 成交     → 终态，不可再流转（仅可转合同）
 * 5 暂停     → 1 恢复跟进、6 中断
 * 6 中断     → 终态，不可恢复
 * </pre>
 *
 * <p>注意：成交只能由【测试中】触发，避免跳过测试直接成交；中断为不可恢复终态。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
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
        map.put(INIT.getCode(), EnumSet.of(FOLLOWING));
        map.put(FOLLOWING.getCode(), EnumSet.of(SUBMIT_TEST, PAUSED, INTERRUPTED));
        map.put(SUBMIT_TEST.getCode(), EnumSet.of(TESTING, FOLLOWING, INTERRUPTED));
        map.put(TESTING.getCode(), EnumSet.of(DEAL, FOLLOWING, PAUSED, INTERRUPTED));
        map.put(DEAL.getCode(), EnumSet.noneOf(BusStatusEnum.class));
        map.put(PAUSED.getCode(), EnumSet.of(FOLLOWING, INTERRUPTED));
        map.put(INTERRUPTED.getCode(), EnumSet.noneOf(BusStatusEnum.class));
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
     * 校验状态码合法性，非法直接返回 null 由调用方抛业务异常
     *
     * @param code 状态码
     * @return 枚举
     */
    public static BusStatusEnum require(Long code)
    {
        return of(code);
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
     * 获取当前状态允许流转到的目标状态列表（按状态码升序）
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
     * 是否为终态
     */
    public boolean isFinal()
    {
        return allowedNextStatuses().isEmpty();
    }

    /**
     * 是否允许编辑商机基础信息
     *
     * <p>仅【初始化】【跟进中】可编辑；其余状态（含提交测试）主信息与产品清单一并锁定。</p>
     *
     * @return 允许编辑返回 true
     */
    public boolean isMainEditable()
    {
        return this == INIT || this == FOLLOWING;
    }

    /**
     * 是否允许维护意向产品清单
     *
     * <p>需求约定：状态 &gt;= 2（提交测试及以后）产品清单锁定，防止报价随意变更。</p>
     *
     * @return 允许维护返回 true
     */
    public boolean isProductEditable()
    {
        return this == INIT || this == FOLLOWING;
    }

    /**
     * 是否允许删除
     *
     * <p>仅【初始化】【跟进中】可删除；成交 / 测试中 / 中断等状态禁止删除。</p>
     *
     * @return 允许删除返回 true
     */
    public boolean isDeletable()
    {
        return this == INIT || this == FOLLOWING;
    }

    /**
     * 是否允许转合同
     *
     * @return 成交状态返回 true
     */
    public boolean isConvertible()
    {
        return this == DEAL;
    }
}
