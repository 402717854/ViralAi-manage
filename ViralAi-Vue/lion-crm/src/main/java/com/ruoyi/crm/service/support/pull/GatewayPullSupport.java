package com.ruoyi.crm.service.support.pull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;

/**
 * 拉取适配器的公共工具
 *
 * <p>三件事：建带超时的 RestTemplate、把计费信息压成 {@code raw_price_detail}（500 字符）、
 * 以及按上游要求切分时间窗口。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
public final class GatewayPullSupport
{
    private static final Logger log = LoggerFactory.getLogger(GatewayPullSupport.class);

    /** gateway_call_log.raw_price_detail 的长度上限 */
    public static final int RAW_DETAIL_MAX_LENGTH = 500;

    /** 单次时间窗口上限：pixverse 明确要求不超过 30 天，其余上游按同一口径切分也无害 */
    public static final int MAX_WINDOW_DAYS = 30;

    private static final ObjectMapper JSON = new ObjectMapper();

    private GatewayPullSupport()
    {
    }

    /**
     * 建带超时的 RestTemplate（每次拉取新建，避免共享连接池带来的超时串扰）
     *
     * @param connectTimeout 连接超时（毫秒）
     * @param readTimeout 读取超时（毫秒）
     * @return RestTemplate
     */
    public static RestTemplate restTemplate(int connectTimeout, int readTimeout)
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    /**
     * URL 编码
     *
     * @param value 原值
     * @return 编码后的值
     */
    public static String encode(String value)
    {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 把计费信息压成一行 JSON 并按字段长度截断
     *
     * <p>截断而不是丢弃：截断后至少能看到积分/额度这类关键数字，
     * 完整原文仍在上游系统里可查。</p>
     *
     * @param detail 计费信息（保持插入顺序）
     * @return JSON 字符串
     */
    public static String toRawDetail(Map<String, Object> detail)
    {
        if (detail == null || detail.isEmpty())
        {
            return null;
        }
        try
        {
            String json = JSON.writeValueAsString(detail);
            if (json.length() <= RAW_DETAIL_MAX_LENGTH)
            {
                return json;
            }
            log.debug("上游计费信息超长已截断，原长度 {}", json.length());
            return json.substring(0, RAW_DETAIL_MAX_LENGTH);
        }
        catch (Exception e)
        {
            // 序列化失败不应阻断拉取，退化为字符串拼接
            return truncate(String.valueOf(detail), RAW_DETAIL_MAX_LENGTH);
        }
    }

    /**
     * 按最大长度截断
     *
     * @param value 原值
     * @param maxLength 最大长度
     * @return 截断后的值
     */
    public static String truncate(String value, int maxLength)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 按最大窗口天数切分区间
     *
     * <p>上游普遍限制单次查询跨度（pixverse 为 30 天），切窗后逐段查询，
     * 避免用户选一个季度就对不上账。</p>
     *
     * @param start 起始时间
     * @param end 结束时间
     * @return 窗口列表，每个窗口为 [start, end]
     */
    public static List<Date[]> splitWindows(Date start, Date end)
    {
        List<Date[]> windows = new ArrayList<>();
        if (start == null || end == null || start.after(end))
        {
            return windows;
        }
        long step = MAX_WINDOW_DAYS * 24L * 60L * 60L * 1000L;
        long cursor = start.getTime();
        while (cursor <= end.getTime())
        {
            long windowEnd = Math.min(cursor + step - 1000L, end.getTime());
            windows.add(new Date[] { new Date(cursor), new Date(windowEnd) });
            cursor = windowEnd + 1000L;
        }
        return windows;
    }

    /**
     * 解析日期，失败返回 null
     *
     * @param value 原值
     * @param patterns 候选格式
     * @return 日期
     */
    public static Date parseDate(String value, String... patterns)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        for (String pattern : patterns)
        {
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(value.trim());
            }
            catch (ParseException ignored)
            {
                // 换下一个格式
            }
        }
        return null;
    }

    /**
     * 解析 Unix 秒级时间戳
     *
     * @param value 原值
     * @return 日期
     */
    public static Date fromEpochSecond(Object value)
    {
        Long epoch = toLong(value);
        if (epoch == null || epoch <= 0)
        {
            return null;
        }
        // 部分上游返回毫秒，按量级区分（1e12 以上视为毫秒）
        return epoch > 100000000000L ? new Date(epoch) : new Date(epoch * 1000L);
    }

    /**
     * 转 long，失败返回 null
     *
     * @param value 原值
     * @return 数值
     */
    public static Long toLong(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty())
        {
            return null;
        }
        try
        {
            return (long) Double.parseDouble(text);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    /**
     * 取 Map 中第一个非空值
     *
     * @param row 行
     * @param keys 候选键
     * @return 值，未命中返回 null
     */
    public static Object pick(Map<?, ?> row, String... keys)
    {
        if (row == null)
        {
            return null;
        }
        for (String key : keys)
        {
            Object value = row.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty())
            {
                return value;
            }
        }
        return null;
    }

    /**
     * 取 Map 中第一个非空字符串
     *
     * @param row 行
     * @param keys 候选键
     * @return 值，未命中返回 null
     */
    public static String pickString(Map<?, ?> row, String... keys)
    {
        Object value = pick(row, keys);
        return value == null ? null : String.valueOf(value).trim();
    }

    /**
     * 新建保序的计费信息容器
     *
     * @return Map
     */
    public static Map<String, Object> newDetail()
    {
        return new LinkedHashMap<>();
    }
}
