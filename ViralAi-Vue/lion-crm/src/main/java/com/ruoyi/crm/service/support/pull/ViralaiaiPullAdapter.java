package com.ruoyi.crm.service.support.pull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;

/**
 * viralai（中转站，new-api 系）流水拉取
 *
 * <p>接口：{@code GET {base}/api/log/}，管理员令牌鉴权。</p>
 * <pre>
 * GET /api/log/?p=1&amp;page_size=100&amp;type=2&amp;username=Mark
 *     &amp;start_timestamp=1793808000&amp;end_timestamp=1793853776
 * Authorization: Bearer {apiKey}
 * </pre>
 *
 * <p>响应：{@code {success, message, data:{items:[...], total, page, page_size}}}，
 * 每条记录含 {@code id / username / model_name / created_at / quota /
 * prompt_tokens / completion_tokens}。</p>
 *
 * <p>两个细节：</p>
 * <ul>
 *   <li>{@code type=2} 只取「消费」日志，充值/管理/错误日志不参与对账；</li>
 *   <li>{@code page_size} 超过 100 会被服务端按 100 处理，这里固定 100，
 *       并以返回条数是否满页判断是否继续翻页，不依赖 {@code total}（部分版本不返回）。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Component
public class ViralaiaiPullAdapter implements GatewayPullAdapter
{
    private static final Logger log = LoggerFactory.getLogger(ViralaiaiPullAdapter.class);

    /** 适配器标识 */
    public static final String CHANNEL = "viralaiai";

    /** 默认接口地址，自建中转站需在账户上覆盖 pull_url */
    private static final String DEFAULT_URL = "https://viralaiai.com/api/log/";

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
            throw new ServiceException("渠道账户未配置 apiKey，无法拉取 viralai 流水（账户ID="
                    + context.getAccountId() + "）");
        }
        RestTemplate restTemplate = GatewayPullSupport.restTemplate(
                context.getConnectTimeout(), context.getReadTimeout());
        List<GatewayPullRow> rows = new ArrayList<>();
        String base = StringUtils.isEmpty(context.getUrl()) ? DEFAULT_URL : context.getUrl();
        String user = context.resolveUser();
        long startSecond = context.getStart().getTime() / 1000L;
        long endSecond = context.getEnd().getTime() / 1000L;

        int page = 1;
        while (page <= context.getMaxPages())
        {
            String url = buildUrl(base, page, context.getPageSize(), user, startSecond, endSecond);
            Map<?, ?> body = request(restTemplate, url, apiKey, context);
            List<?> items = extractItems(body);
            if (items.isEmpty())
            {
                break;
            }
            for (Object item : items)
            {
                if (item instanceof Map)
                {
                    GatewayPullRow row = convert((Map<?, ?>) item);
                    if (row != null)
                    {
                        rows.add(row);
                    }
                }
            }
            if (items.size() < context.getPageSize())
            {
                break;
            }
            page++;
            if (page > context.getMaxPages())
            {
                log.warn("viralai 拉取达到最大翻页数 {}，可能有数据未取完，账户ID={}，区间 {} ~ {}",
                        context.getMaxPages(), context.getAccountId(), startSecond, endSecond);
            }
        }
        log.info("viralai 拉取完成，账户ID={}，区间 {} ~ {}，共 {} 页 {} 条",
                context.getAccountId(), startSecond, endSecond, page, rows.size());
        return rows;
    }

    /**
     * 拼装查询地址
     *
     * @param base 基础地址
     * @param page 页码（从 1 开始）
     * @param pageSize 每页条数
     * @param user 用户名过滤，可为空
     * @param startSecond 起始时间戳（秒）
     * @param endSecond 结束时间戳（秒）
     * @return 完整地址
     */
    private String buildUrl(String base, int page, int pageSize, String user, long startSecond, long endSecond)
    {
        StringBuilder url = new StringBuilder(base.trim());
        if (!url.toString().contains("?"))
        {
            url.append('?');
        }
        else if (!url.toString().endsWith("?") && !url.toString().endsWith("&"))
        {
            url.append('&');
        }
        url.append("p=").append(page)
           .append("&page_size=").append(pageSize)
           .append("&type=2")
           .append("&start_timestamp=").append(startSecond)
           .append("&end_timestamp=").append(endSecond);
        if (StringUtils.isNotEmpty(user))
        {
            url.append("&username=").append(GatewayPullSupport.encode(user));
        }
        return url.toString();
    }

    /**
     * 发起一次请求并校验业务成功标记
     *
     * @param restTemplate HTTP 客户端
     * @param url 地址
     * @param apiKey 管理员令牌
     * @param context 上下文
     * @return 响应体
     */
    private Map<?, ?> request(RestTemplate restTemplate, String url, String apiKey, GatewayPullContext context)
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.setBearerAuth(apiKey);
        try
        {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,
                    new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null)
            {
                throw new ServiceException("viralai 返回空响应，账户ID=" + context.getAccountId());
            }
            Object success = body.get("success");
            if (success instanceof Boolean && !((Boolean) success))
            {
                // 令牌失效/权限不足会走到这里，必须显式抛出，否则会被误判为「区间内没有流水」
                throw new ServiceException("viralai 拉取失败：" + body.get("message")
                        + "（账户ID=" + context.getAccountId() + "）");
            }
            return body;
        }
        catch (RestClientException e)
        {
            throw new ServiceException("viralai 接口调用异常：" + e.getMessage()
                    + "（账户ID=" + context.getAccountId() + "）");
        }
    }

    /**
     * 取 items 数组，兼容 data 为数组的旧版本
     *
     * @param body 响应体
     * @return 记录列表
     */
    private List<?> extractItems(Map<?, ?> body)
    {
        Object data = body.get("data");
        if (data instanceof List)
        {
            return (List<?>) data;
        }
        if (data instanceof Map)
        {
            Object items = ((Map<?, ?>) data).get("items");
            if (items instanceof List)
            {
                return (List<?>) items;
            }
        }
        return new ArrayList<>();
    }

    /**
     * 单条记录转流水行
     *
     * @param item 原始记录
     * @return 流水行，缺关键字段返回 null
     */
    private GatewayPullRow convert(Map<?, ?> item)
    {
        String id = GatewayPullSupport.pickString(item, "id", "log_id", "request_id");
        Date callTime = GatewayPullSupport.fromEpochSecond(GatewayPullSupport.pick(item, "created_at", "createdAt"));
        String model = GatewayPullSupport.pickString(item, "model_name", "modelName", "model");
        if (StringUtils.isEmpty(id) || callTime == null || StringUtils.isEmpty(model))
        {
            log.warn("viralai 记录缺少 id/created_at/model_name，已跳过：{}", item.keySet());
            return null;
        }
        GatewayPullRow row = new GatewayPullRow();
        row.setUpstreamId(id);
        row.setUpstreamUser(GatewayPullSupport.pickString(item, "username", "user_name"));
        row.setModelKey(model);
        row.setModelVariant("");
        row.setCallTime(callTime);
        Long prompt = GatewayPullSupport.toLong(GatewayPullSupport.pick(item, "prompt_tokens", "promptTokens"));
        Long completion = GatewayPullSupport.toLong(
                GatewayPullSupport.pick(item, "completion_tokens", "completionTokens"));
        row.setPromptTokens(prompt == null ? 0L : Math.max(prompt, 0L));
        row.setCompletionTokens(completion == null ? 0L : Math.max(completion, 0L));
        row.setTotalTokens(row.getPromptTokens() + row.getCompletionTokens());
        Integer quota = (Integer) GatewayPullSupport.pick(item, "quota");
        row.setTotalFees(null);
        row.setActualFees(new BigDecimal(quota).divide(new BigDecimal(500000),9, RoundingMode.HALF_UP).multiply(new BigDecimal(7)));
        row.setModelType("LLM");

        // 原始计费信息：quota 是中转站的额度单位，不能直接当金额，原样留档供核对
        Map<String, Object> detail = GatewayPullSupport.newDetail();
        detail.put("source", CHANNEL);
        detail.put("quota", GatewayPullSupport.pick(item, "quota"));
        detail.put("useTime", GatewayPullSupport.pick(item, "use_time", "useTime"));
        detail.put("tokenName", GatewayPullSupport.pick(item, "token_name", "tokenName"));
        detail.put("channel", GatewayPullSupport.pick(item, "channel", "channel_id"));
        detail.put("group", GatewayPullSupport.pick(item, "group"));
        detail.put("requestId", GatewayPullSupport.pick(item, "request_id", "requestId"));
        row.setRawPriceDetail(GatewayPullSupport.toRawDetail(detail));
        return row;
    }
}
