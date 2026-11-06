package com.ruoyi.crm.service.support.pull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 * 通用网关拉取（兼容旧的全局配置方式）
 *
 * <p>账户未配置 {@code pull_channel} 时使用：向 {@code crm.gateway.pull-url} POST
 * {@code {startTime, endTime, supplierId}}，期望返回 {@code {"data":[...]}} 或
 * {@code {"rows":[...]}}，字段兼容驼峰与下划线。</p>
 *
 * <p>保留这个适配器是为了不破坏已有的自建网关联调方式；新增渠道请直接实现
 * {@link GatewayPullAdapter} 并指定 {@code pull_channel}，不要往这里加分支。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
//@Component
public class GenericPullAdapter implements GatewayPullAdapter
{
    private static final Logger log = LoggerFactory.getLogger(GenericPullAdapter.class);

    /** 适配器标识：账户 pull_channel 为空时由工厂回退到此实现 */
    public static final String CHANNEL = "generic";

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 通用拉取地址，为空表示该账户未配置拉取能力 */
    @Value("${crm.gateway.pull-url:}")
    private String pullUrl;

    /** 通用拉取令牌 */
    @Value("${crm.gateway.pull-token:}")
    private String pullToken;

    @Override
    public String channel()
    {
        return CHANNEL;
    }

    @Override
    public String defaultUrl()
    {
        return pullUrl;
    }

    @Override
    public List<GatewayPullRow> pull(GatewayPullContext context)
    {
        String url = StringUtils.isNotEmpty(context.getUrl()) ? context.getUrl() : pullUrl;
        if (StringUtils.isEmpty(url))
        {
            throw new ServiceException("渠道账户未配置拉取协议（pull_channel），"
                    + "且未配置通用地址 crm.gateway.pull-url；请为该账户选择拉取适配器"
                    + "（账户ID=" + context.getAccountId() + "）");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String token = StringUtils.isNotEmpty(context.resolveApiKey()) ? context.resolveApiKey() : pullToken;
        if (StringUtils.isNotEmpty(token))
        {
            headers.setBearerAuth(token);
        }

        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_PATTERN);
        Map<String, Object> body = new HashMap<>();
        body.put("startTime", format.format(context.getStart()));
        body.put("endTime", format.format(context.getEnd()));
        if (context.getSupplierId() != null)
        {
            body.put("supplierId", context.getSupplierId());
        }

        RestTemplate restTemplate = GatewayPullSupport.restTemplate(
                context.getConnectTimeout(), context.getReadTimeout());
        try
        {
            ResponseEntity<Map> response = restTemplate.postForEntity(url,
                    new HttpEntity<>(body, headers), Map.class);
            Map<?, ?> result = response.getBody();
            Object data = result == null ? null
                    : (result.containsKey("data") ? result.get("data") : result.get("rows"));
            if (!(data instanceof List))
            {
                log.warn("通用网关拉取返回结构不符合预期，未取到 data/rows 数组，url={}", url);
                return new ArrayList<>();
            }
            List<GatewayPullRow> rows = new ArrayList<>();
            for (Object row : (List<?>) data)
            {
                if (row instanceof Map)
                {
                    GatewayPullRow converted = convert((Map<?, ?>) row);
                    if (converted != null)
                    {
                        rows.add(converted);
                    }
                }
            }
            return rows;
        }
        catch (RestClientException e)
        {
            throw new ServiceException("通用网关拉取异常：" + e.getMessage()
                    + "（账户ID=" + context.getAccountId() + "）");
        }
    }

    /**
     * 通用行转流水行
     *
     * @param row 原始行
     * @return 流水行
     */
    private GatewayPullRow convert(Map<?, ?> row)
    {
        String id = GatewayPullSupport.pickString(row, "callNo", "call_no", "id");
        Date callTime = GatewayPullSupport.parseDate(
                GatewayPullSupport.pickString(row, "callTime", "call_time"), DATE_TIME_PATTERN);
        if (StringUtils.isEmpty(id) || callTime == null)
        {
            return null;
        }
        GatewayPullRow result = new GatewayPullRow();
        result.setUpstreamId(id);
        result.setModelKey(GatewayPullSupport.pickString(row, "productCode", "product_code",
                "modelCode", "model_code"));
        result.setModelVariant("");
        result.setCallTime(callTime);
        Long prompt = GatewayPullSupport.toLong(GatewayPullSupport.pick(row, "promptTokens", "prompt_tokens"));
        Long completion = GatewayPullSupport.toLong(
                GatewayPullSupport.pick(row, "completionTokens", "completion_tokens"));
        result.setPromptTokens(prompt == null ? 0L : prompt);
        result.setCompletionTokens(completion == null ? 0L : completion);
        Long total = GatewayPullSupport.toLong(GatewayPullSupport.pick(row, "totalTokens", "total_tokens"));
        result.setTotalTokens(total == null ? result.getPromptTokens() + result.getCompletionTokens() : total);
        result.setModelType(GatewayPullSupport.pickString(row, "modelType", "model_type"));
        Long status = GatewayPullSupport.toLong(GatewayPullSupport.pick(row, "callStatus", "call_status"));
        result.setCallStatus(status == null ? 1 : status.intValue());
        result.setErrMsg(GatewayPullSupport.pickString(row, "errMsg", "err_msg"));
        result.setRawPriceDetail(GatewayPullSupport.truncate(
                GatewayPullSupport.pickString(row, "rawPriceDetail", "raw_price_detail"),
                GatewayPullSupport.RAW_DETAIL_MAX_LENGTH));
        return result;
    }
}
