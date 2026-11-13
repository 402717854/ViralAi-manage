package com.ruoyi.crm.service.support.pull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * pixverse 用量明细拉取
 *
 * <p>接口：{@code POST {base}/openapi/v2/account/billing/usage-detail}，
 * 请求头 {@code API-KEY}（另有 {@code Ai-trace-id} 便于上游排查）。</p>
 *
 * <p>与中转站的三处关键差异：</p>
 * <ul>
 *   <li><b>游标分页</b>：返回 {@code Resp.has_more / Resp.next_cursor}，下次请求把游标原样回填，
 *       且带上游标后其他筛选条件会被服务端忽略；</li>
 *   <li><b>区间上限 30 天</b>：超出必须切窗，否则直接报错；</li>
 *   <li><b>没有客户维度</b>：记录里只有 record_id / model / quality / credits，
 *       客户只能按「账户默认客户」映射确定。</li>
 * </ul>
 *
 * <p>计量单位是 credits 而不是 token，因此 {@code total_tokens} 恒为 0，
 * credits 原样写入 {@code raw_price_detail} 留档。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Component
public class PixversePullAdapter implements GatewayPullAdapter
{
    private static final Logger log = LoggerFactory.getLogger(PixversePullAdapter.class);

    /** 适配器标识 */
    public static final String CHANNEL = "pixverse";

    /**
     * 默认接口地址
     * <p>官方有 .ai（国际）与 .cn（国内）两套域名，API Key 必须与域名同地域；
     * 默认给 .ai，国内账户在渠道账户上覆盖 pull_url 即可。</p>
     */
    private static final String DEFAULT_URL =
            "https://app-api.pixverse.ai/openapi/v2/account/billing/usage-detail";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Override
    public String channel()
    {
        return CHANNEL;
    }

    @Override
    public String defaultUrl()
    {
        return DEFAULT_URL;
    }

    @Override
    public List<GatewayPullRow> pull(GatewayPullContext context)
    {
        String apiKey = context.resolveApiKey();
        if (StringUtils.isEmpty(apiKey))
        {
            throw new ServiceException("渠道账户未配置 apiKey，无法拉取 pixverse 用量（账户ID="
                    + context.getAccountId() + "）");
        }
        String url = StringUtils.isEmpty(context.getUrl()) ? DEFAULT_URL : context.getUrl();
        RestTemplate restTemplate = GatewayPullSupport.restTemplate(
                context.getConnectTimeout(), context.getReadTimeout());

        List<GatewayPullRow> rows = new ArrayList<>();
        int skipped = 0;
        // 单次查询跨度不得超过 30 天，超长区间先切窗
        for (Date[] window : GatewayPullSupport.splitWindows(context.getStart(), context.getEnd()))
        {
            String cursor = "";
            int page = 0;
            while (page < context.getMaxPages())
            {
                Map<?, ?> body = request(restTemplate, url, apiKey, context, window[0], window[1], cursor);
                Map<?, ?> resp = body.get("Resp") instanceof Map ? (Map<?, ?>) body.get("Resp") : null;
                if (resp == null)
                {
                    break;
                }
                Object items = resp.get("items");
                if (items instanceof List)
                {
                    for (Object item : (List<?>) items)
                    {
                        if (!(item instanceof Map))
                        {
                            continue;
                        }
                        Map<?, ?> map = (Map<?, ?>) item;
                        String eventType = GatewayPullSupport.pickString(map, "event_type", "eventType");
                        if (StringUtils.isNotEmpty(eventType) && !"consume".equalsIgnoreCase(eventType))
                        {
                            // 返还（refund）不能当成一次用量入账，否则会重复计费
                            skipped++;
                            continue;
                        }
                        GatewayPullRow row = convert(map);
                        if (row != null)
                        {
                            rows.add(row);
                        }
                    }
                }
                page++;
                Object hasMore = resp.get("has_more");
                if (!(hasMore instanceof Boolean) || !((Boolean) hasMore))
                {
                    break;
                }
                String next = GatewayPullSupport.pickString(resp, "next_cursor", "nextCursor");
                if (StringUtils.isEmpty(next) || next.equals(cursor))
                {
                    // 上游声称还有下一页但游标没变，继续翻会死循环，必须停
                    log.warn("pixverse 返回 has_more=true 但游标为空或未推进，停止翻页，账户ID={}",
                            context.getAccountId());
                    break;
                }
                cursor = next;
            }
        }
        log.info("pixverse 拉取完成，账户ID={}，区间 {} ~ {}，共 {} 条（跳过返还/非消费记录 {} 条）",
                context.getAccountId(), format(context.getStart()), format(context.getEnd()), rows.size(), skipped);
        return rows;
    }

    /**
     * 发起一次请求
     *
     * @param restTemplate HTTP 客户端
     * @param url 地址
     * @param apiKey 密钥
     * @param context 上下文
     * @param start 窗口起
     * @param end 窗口止
     * @param cursor 游标，首次为空
     * @return 响应体
     */
    private Map<?, ?> request(RestTemplate restTemplate, String url, String apiKey, GatewayPullContext context,
                              Date start, Date end, String cursor)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set("API-KEY", apiKey);
        headers.set("Ai-trace-id", UUID.randomUUID().toString());

        Map<String, Object> body = new HashMap<>();
        body.put("start_time", format(start));
        body.put("end_time", format(end));
        body.put("creation_type", new ArrayList<>());
        body.put("limit", context.getPageSize());
        if (StringUtils.isNotEmpty(cursor))
        {
            body.put("cursor", cursor);
        }
        try
        {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            Map<?, ?> result = response.getBody();
            if (result == null)
            {
                throw new ServiceException("pixverse 返回空响应，账户ID=" + context.getAccountId());
            }
            // HTTP 200 不代表业务成功，必须校验 ErrCode
            Long errCode = GatewayPullSupport.toLong(result.get("ErrCode"));
            if (errCode == null || errCode != 0L)
            {
                throw new ServiceException("pixverse 拉取失败：" + result.get("ErrMsg")
                        + "（ErrCode=" + result.get("ErrCode") + "，账户ID=" + context.getAccountId() + "）");
            }
            return result;
        }
        catch (RestClientException e)
        {
            throw new ServiceException("pixverse 接口调用异常：" + e.getMessage()
                    + "（账户ID=" + context.getAccountId() + "）");
        }
    }

    /**
     * 单条记录转流水行
     *
     * @param item 原始记录
     * @return 流水行，缺关键字段返回 null
     */
    private GatewayPullRow convert(Map<?, ?> item)
    {
        String id = GatewayPullSupport.pickString(item, "record_id", "recordId");
        Date callTime = GatewayPullSupport.parseDate(
                GatewayPullSupport.pickString(item, "created_at", "createdAt"), DATE_TIME_PATTERN);
        String model = GatewayPullSupport.pickString(item, "model");
        if (StringUtils.isEmpty(id) || callTime == null || StringUtils.isEmpty(model))
        {
            log.warn("pixverse 记录缺少 record_id/created_at/model，已跳过：{}", item.keySet());
            return null;
        }
        String quality = GatewayPullSupport.pickString(item, "quality");
        String creationType = GatewayPullSupport.pickString(item, "creation_type", "creationType");

        GatewayPullRow row = new GatewayPullRow();
        row.setUpstreamId(id);
        row.setModelKey(model);
        row.setModelVariant(buildVariant(quality, creationType));
        row.setCallTime(callTime);
        // credits 计量，没有 token 概念；换算规则未定前不臆造 token 数
        row.setPromptTokens(0L);
        row.setCompletionTokens(0L);
        row.setTotalTokens(0L);
        row.setTotalFees(null);
        row.setActualFees(null);
        int credits=(int)GatewayPullSupport.pick(item, "credits");
        if(credits<0){
            row.setTotalTokens(0L-credits);
            row.setTotalFees(new BigDecimal(0-credits).multiply(new BigDecimal("0.0293")));
        }
        row.setModelType(resolveModelType(creationType));

        Map<String, Object> detail = GatewayPullSupport.newDetail();
        detail.put("source", CHANNEL);
        detail.put("credits", GatewayPullSupport.pick(item, "credits"));
        detail.put("eventType", GatewayPullSupport.pick(item, "event_type", "eventType"));
        detail.put("creditSource", GatewayPullSupport.pick(item, "credit_source", "creditSource"));
        detail.put("model", model);
        detail.put("quality", quality);
        detail.put("creationType", creationType);
        detail.put("durationSeconds", GatewayPullSupport.pick(item, "billing_duration_seconds", "billingDurationSeconds"));
        detail.put("videoId", GatewayPullSupport.pick(item, "video_id", "videoId"));
        detail.put("imageId", GatewayPullSupport.pick(item, "image_id", "imageId"));
        detail.put("apiKeyName", GatewayPullSupport.pick(item, "api_key_name", "apiKeyName"));
        row.setRawPriceDetail(GatewayPullSupport.toRawDetail(detail));
        return row;
    }

    /**
     * 变体键：清晰度|能力类型，供映射表匹配（价格随清晰度与能力变化）
     *
     * @param quality 清晰度
     * @param creationType 能力类型
     * @return 变体键
     */
    private String buildVariant(String quality, String creationType)
    {
        return (quality == null ? "" : quality) + "|" + (creationType == null ? "" : creationType);
    }

    /**
     * 推断模型类型，仅用于列表展示与排查
     *
     * @param creationType 能力类型
     * @return 模型类型
     */
    private String resolveModelType(String creationType)
    {
        if (StringUtils.isEmpty(creationType))
        {
            return null;
        }
        if (creationType.contains("image"))
        {
            return "Image";
        }
        return "Video";
    }

    /**
     * 格式化时间（上游要求 UTC+8 的 yyyy-MM-dd HH:mm:ss）
     *
     * @param date 时间
     * @return 字符串
     */
    private String format(Date date)
    {
        return new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
