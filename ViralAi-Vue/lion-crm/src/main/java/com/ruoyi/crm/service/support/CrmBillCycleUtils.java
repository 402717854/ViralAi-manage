package com.ruoyi.crm.service.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * 账期工具
 *
 * <p>账期统一使用 {@code yyyy-MM}；区间取自然月，起始 00:00:00.000、结束 23:59:59.999，
 * 保证跨月调用不重复、不遗漏。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public class CrmBillCycleUtils
{
    private static final String CYCLE_PATTERN = "yyyy-MM";

    private CrmBillCycleUtils()
    {
    }

    /**
     * 当前账期
     *
     * @return yyyy-MM
     */
    public static String currentCycle()
    {
        return new SimpleDateFormat(CYCLE_PATTERN).format(new Date());
    }

    /**
     * 上一账期（跨年自动处理）
     *
     * @return yyyy-MM
     */
    public static String lastCycle()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return new SimpleDateFormat(CYCLE_PATTERN).format(calendar.getTime());
    }

    /**
     * 校验并规范化账期
     *
     * @param billCycle 账期，为空时取当前账期
     * @return yyyy-MM
     */
    public static String normalize(String billCycle)
    {
        if (StringUtils.isEmpty(billCycle))
        {
            return currentCycle();
        }
        String value = billCycle.trim();
        SimpleDateFormat format = new SimpleDateFormat(CYCLE_PATTERN);
        format.setLenient(false);
        try
        {
            format.parse(value);
        }
        catch (ParseException e)
        {
            throw new ServiceException("账期格式错误，应为 yyyy-MM，例如 2026-11");
        }
        return value;
    }

    /**
     * 计算账期区间
     *
     * @param billCycle 账期 yyyy-MM
     * @return 数组 [起始时间, 结束时间]
     */
    public static Date[] rangeOf(String billCycle)
    {
        String cycle = normalize(billCycle);
        Date start;
        Date end;
        try
        {
            start = new SimpleDateFormat(CYCLE_PATTERN).parse(cycle);
        }
        catch (ParseException e)
        {
            throw new ServiceException("账期格式错误，应为 yyyy-MM，例如 2026-11");
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date begin = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        end = calendar.getTime();

        return new Date[] { begin, end };
    }

    /**
     * 解析对账区间起始时间
     *
     * <p>支持 {@code yyyy-MM-dd} 与 {@code yyyy-MM-dd HH:mm:ss}；为空时取「昨天 00:00:00」，
     * 对应 T+1 对账口径。</p>
     *
     * @param value 时间字符串
     * @return 起始时间
     */
    public static Date parseStart(String value)
    {
        Date parsed = parse(value);
        if (parsed == null)
        {
            return truncateDay(yesterday(), 0);
        }
        return truncateDay(parsed, 0);
    }

    /**
     * 解析对账区间结束时间
     *
     * <p>只传日期时补 23:59:59；为空时取「昨天 23:59:59」。</p>
     *
     * @param value 时间字符串
     * @return 结束时间
     */
    public static Date parseEnd(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return truncateDay(yesterday(), 23, 59, 59);
        }
        String text = value.trim();
        Date parsed = parse(text);
        if (parsed == null)
        {
            return truncateDay(yesterday(), 23, 59, 59);
        }
        return truncateDay(parsed, 23, 59, 59);
    }

    /**
     * 昨天同一时刻
     *
     * @return 日期
     */
    public static Date yesterday()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTime();
    }

    /**
     * 解析日期，无法解析返回 null
     *
     * @param value 字符串
     * @return 日期
     */
    private static Date parse(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        String text = value.trim();
        String pattern = text.length() <= 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        try
        {
            return format.parse(text);
        }
        catch (ParseException e)
        {
            throw new ServiceException("时间格式错误，应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss：" + text);
        }
    }

    /**
     * 截断到指定时分秒
     *
     * @param date 日期
     * @param values 时分秒，长度为 1 时表示只补 0 点
     * @return 截断后的时间
     */
    private static Date truncateDay(Date date, int... values)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = values.length > 0 ? values[0] : 0;
        int minute = values.length > 1 ? values[1] : 0;
        int second = values.length > 2 ? values[2] : 0;
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
