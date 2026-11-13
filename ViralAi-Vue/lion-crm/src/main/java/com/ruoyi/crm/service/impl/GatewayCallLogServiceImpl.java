package com.ruoyi.crm.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.mapper.GatewayCallLogMapper;
import com.ruoyi.crm.service.IGatewayCallLogService;
import com.ruoyi.crm.service.support.CrmBillCycleUtils;
import com.ruoyi.crm.service.support.CrmFinanceDataScope;
import com.ruoyi.crm.service.support.pull.GatewayPullAdapter;
import com.ruoyi.crm.service.support.pull.GatewayPullAdapterFactory;
import com.ruoyi.crm.service.support.pull.GatewayPullContext;
import com.ruoyi.crm.service.support.pull.GatewayPullMapping;
import com.ruoyi.crm.service.support.pull.GatewayPullMappingLoader;
import com.ruoyi.crm.service.support.pull.GatewayPullRow;
import com.ruoyi.crm.service.support.pull.GatewayPullSupport;

/**
 * 网关调用流水Service实现
 *
 * <p>入库幂等依赖 {@code gateway_call_log.uk_call_no}：重复推送直接跳过并计入 duplicated，
 * 不会重复计费（需求文档第八章风险 1）。</p>
 *
 * <p>拉取按「渠道 + 渠道账户」进行：一个账户 = 一套密钥 + 一种上游协议，
 * 协议差异封装在 {@link GatewayPullAdapter} 的实现里（viralai 中转站、pixverse、通用）。
 * 本类只负责三件与上游无关的事：确定拉取范围、把上游标识映射成本系统主键、
 * 以及统一入库。映射规则见 {@code crm_gateway_pull_map}。</p>
 *
 * <p><b>客户解析不出来的流水不入库</b>：宁可少入库让运维看到「未映射」计数，
 * 也不能把流水挂到一个猜出来的客户头上，那会直接算错账。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class GatewayCallLogServiceImpl implements IGatewayCallLogService
{
    private static final Logger log = LoggerFactory.getLogger(GatewayCallLogServiceImpl.class);

    /** gateway_call_log.product_code 长度 */
    private static final int MAX_PRODUCT_CODE = 64;

    /** gateway_call_log.err_msg 长度 */
    private static final int MAX_ERR_MSG = 500;

    /** 未映射用户名样例上限，避免一次拉取把几万个用户名塞进响应 */
    private static final int MAX_UNMAPPED_SAMPLE = 20;

    /** 流水拉取方式：2 文件上传（人工上传流水文件，不参与路由拉取） */
    private static final Integer PULL_TYPE_UPLOAD = 2;

    @Autowired
    private GatewayCallLogMapper gatewayCallLogMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmFinanceDataScope crmFinanceDataScope;

    @Autowired
    private GatewayPullAdapterFactory gatewayPullAdapterFactory;

    @Autowired
    private GatewayPullMappingLoader gatewayPullMappingLoader;

    /** 每页条数 */
    @Value("${crm.gateway.pull.page-size:100}")
    private int pullPageSize;

    /** 最大翻页数，防止上游分页异常导致死循环 */
    @Value("${crm.gateway.pull.max-pages:200}")
    private int pullMaxPages;

    /** 连接超时（毫秒） */
    @Value("${crm.gateway.pull.connect-timeout:5000}")
    private int pullConnectTimeout;

    /** 读取超时（毫秒） */
    @Value("${crm.gateway.pull.read-timeout:60000}")
    private int pullReadTimeout;

    @Override
    public List<GatewayCallLogDto> selectGatewayCallLogList(GatewayCallLogDto query)
    {
        crmFinanceDataScope.applyScope(query);
        return gatewayCallLogMapper.selectGatewayCallLogList(query);
    }

    @Override
    public Map<String, Object> ingest(List<GatewayCallLog> logs)
    {
        Map<String, Object> result = new HashMap<>();
        int inserted = 0;
        int duplicated = 0;
        int invalid = 0;
        if (logs != null)
        {
            for (GatewayCallLog item : logs)
            {
                if (!isValid(item))
                {
                    invalid++;
                    continue;
                }
                normalize(item);
                try
                {
                    gatewayCallLogMapper.insertGatewayCallLog(item);
                    inserted++;
                }
                catch (DataIntegrityViolationException e)
                {
                    // 唯一索引冲突即重复推送，幂等跳过
                    duplicated++;
                }
            }
        }
        result.put("inserted", inserted);
        result.put("duplicated", duplicated);
        result.put("invalid", invalid);
        result.put("total", inserted + duplicated + invalid);
        return result;
    }

    /**
     * 按渠道 / 渠道账户拉取流水并入库
     *
     * <p>范围规则：只给 accountId 拉该账户；只给 supplierId 拉该渠道下所有启用账户；
     * 都不给拉全部启用账户（定时任务）。单个账户失败不阻断其他账户，但全部失败时抛错，
     * 避免运维看到「成功 0 条」却以为区间内本来就没有流水。</p>
     */
    @Override
    public Map<String, Object> pullAndIngest(String startTime, String endTime, Long supplierId,
                                             Long supplierAccountId)
    {
        Date start = CrmBillCycleUtils.parseStart(startTime);
        Date end = CrmBillCycleUtils.parseEnd(endTime);
        if (start == null || end == null)
        {
            throw new ServiceException("拉取区间不能为空");
        }
        if (start.after(end))
        {
            throw new ServiceException("拉取起始时间不能晚于结束时间");
        }

        List<CrmSupplierAccount> accounts = crmSupplierAccountMapper.selectAccountsForPull(supplierId,
                supplierAccountId);
        if (accounts == null || accounts.isEmpty())
        {
            throw new ServiceException("未找到可拉取的渠道账户"
                    + (supplierAccountId != null ? "（账户ID=" + supplierAccountId + "）"
                            : supplierId != null ? "（渠道ID=" + supplierId + "，且需为启用状态）"
                                    : "（没有任何启用账户）"));
        }
        if (supplierAccountId != null && supplierId != null)
        {
            CrmSupplierAccount account = accounts.get(0);
            if (!supplierId.equals(account.getSupplierId()))
            {
                throw new ServiceException("账户 " + supplierAccountId + " 不属于渠道 " + supplierId);
            }
        }
        // 映射规则一次载入内存，避免逐条流水查库
        GatewayPullMapping mapping = gatewayPullMappingLoader.load(accounts);

        List<GatewayCallLog> logs = new ArrayList<>();
        List<Map<String, Object>> accountResults = new ArrayList<>();
        Set<String> unmappedUsers = new LinkedHashSet<>();
        Set<String> missingModels = new LinkedHashSet<>();
        int unmapped = 0;
        int missingModel = 0;
        int failed = 0;
        // 拉取方式为「文件上传」的账户不参与路由拉取，单独计数，避免被误判为拉取失败
        int skipped = 0;

        for (CrmSupplierAccount account : accounts)
        {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("accountId", account.getId());
            one.put("accountName", account.getAccountName());
            one.put("supplierId", account.getSupplierId());
            one.put("pullType", account.getPullType());
            one.put("pullChannel", account.getPullChannel());
            if (PULL_TYPE_UPLOAD.equals(account.getPullType()))
            {
                skipped++;
                one.put("skipped", true);
                one.put("reason", "拉取方式为文件上传，跳过路由拉取");
                accountResults.add(one);
                continue;
            }
            try
            {
                GatewayPullAdapter adapter = gatewayPullAdapterFactory.get(account.getPullChannel());
                GatewayPullContext context = buildContext(account, adapter, start, end);
                List<GatewayPullRow> rows = adapter.pull(context);
                one.put("pulled", rows.size());

                String prefix = callNoPrefix(adapter);
                int accountUnmapped = 0;
                int accountMissingModel = 0;
                int added = 0;
                for (GatewayPullRow row : rows)
                {
                    Long customerId = mapping.resolveCustomerId(account.getSupplierId(), account.getId(),
                            row.getUpstreamUser());
                    if (customerId == null)
                    {
                        accountUnmapped++;
                        if (unmappedUsers.size() < MAX_UNMAPPED_SAMPLE)
                        {
                            unmappedUsers.add(StringUtils.isEmpty(row.getUpstreamUser())
                                    ? ("<账户默认客户未配置:" + account.getId() + ">")
                                    : row.getUpstreamUser());
                        }
                        continue;
                    }
                    String productCode = mapping.resolveProductCode(account.getSupplierId(), account.getId(),
                            row.getModelKey(), row.getModelVariant());
                    if (StringUtils.isEmpty(productCode))
                    {
                        productCode = fallbackProductCode(row);
                    }
                    if (StringUtils.isEmpty(productCode))
                    {
                        accountMissingModel++;
                        if (missingModels.size() < MAX_UNMAPPED_SAMPLE)
                        {
                            missingModels.add(String.valueOf(row.getUpstreamId()));
                        }
                        continue;
                    }
                    logs.add(toCallLog(row, account, customerId, productCode, prefix));
                    added++;
                }
                unmapped += accountUnmapped;
                missingModel += accountMissingModel;
                one.put("added", added);
                one.put("unmapped", accountUnmapped);
                one.put("missingModel", accountMissingModel);
                if (accountUnmapped > 0)
                {
                    log.warn("拉取到 {} 条流水无法确定客户（账户ID={}，账户名={}），请在 crm_gateway_pull_map 补充映射",
                            accountUnmapped, account.getId(), account.getAccountName());
                }
            }
            catch (Exception e)
            {
                // 单账户失败不影响其他账户；错误回填到明细，便于定位是哪个账户的问题
                failed++;
                one.put("error", e.getMessage());
                log.error("渠道账户拉取失败，账户ID={}，账户名={}，渠道={}", account.getId(),
                        account.getAccountName(), account.getPullChannel(), e);
            }
            accountResults.add(one);
        }

        int attempted = accounts.size() - skipped;
        if (attempted == 0)
        {
            // 全部为文件上传账户：不存在可路由拉取的账户，直接给出结论而不是「成功 0 条」
            throw new ServiceException("所选范围内没有采用「路由获取」方式的渠道账户，"
                    + "文件上传方式的账户由人工上传流水文件，不参与路由拉取");
        }
        if (failed > 0 && failed == attempted)
        {
            // 首个账户可能被跳过而没有 error，需取第一个真正失败的明细
            Object firstError = null;
            for (Map<String, Object> item : accountResults)
            {
                if (item.get("error") != null)
                {
                    firstError = item.get("error");
                    break;
                }
            }
            throw new ServiceException("全部渠道账户拉取失败，请检查密钥与接口地址，首个错误：" + firstError);
        }

        Map<String, Object> result = ingest(logs);
        result.put("pulled", logs.size() + unmapped + missingModel);
        result.put("unmapped", unmapped);
        result.put("unmappedSample", new ArrayList<>(unmappedUsers));
        result.put("missingModel", missingModel);
        result.put("missingModelSample", new ArrayList<>(missingModels));
        result.put("skipped", skipped);
        result.put("accounts", accountResults);
        result.put("startTime", start);
        result.put("endTime", end);
        log.info("流水拉取完成，区间 {} ~ {}，账户 {} 个（失败 {} 个、文件上传跳过 {} 个），拉取 {} 条，未映射客户 {} 条，"
                        + "缺模型编码 {} 条，入库 {} 条 / 重复 {} 条",
                start, end, accounts.size(), failed, skipped, result.get("pulled"), unmapped, missingModel,
                result.get("inserted"), result.get("duplicated"));
        return result;
    }

    /**
     * 构建拉取上下文
     *
     * @param account 渠道账户
     * @param adapter 适配器
     * @param start 起始时间
     * @param end 结束时间
     * @return 上下文
     */
    private GatewayPullContext buildContext(CrmSupplierAccount account, GatewayPullAdapter adapter,
                                            Date start, Date end)
    {
        GatewayPullContext context = new GatewayPullContext();
        context.setAccount(account);
        context.setStart(start);
        context.setEnd(end);
        context.setUrl(gatewayPullAdapterFactory.resolveUrl(account.getPullUrl(), adapter));
        context.setPageSize(pullPageSize <= 0 ? 100 : pullPageSize);
        context.setMaxPages(pullMaxPages <= 0 ? 200 : pullMaxPages);
        context.setConnectTimeout(pullConnectTimeout);
        context.setReadTimeout(pullReadTimeout);
        return context;
    }

    /**
     * 流水号前缀
     *
     * <p>上游 ID 只在上游内部唯一，加渠道前缀避免不同上游撞车。通用适配器的上游
     * 已经是本系统流水号（上报模式），不能再加前缀，否则同一条流水会因前缀不同重复入账。</p>
     *
     * @param adapter 适配器
     * @return 前缀，通用适配器返回空串
     */
    private String callNoPrefix(GatewayPullAdapter adapter)
    {
        if (adapter == null)
        {
            return "";
        }
        return adapter.channel().toUpperCase() + "-";
    }

    /**
     * 未配置模型映射时的退化编码：模型 + 变体
     *
     * <p>退化成「上游自己的名字」而不是空值，是为了让对账引擎给出「模型不在合同内」的
     * 明确结论，运维据此补映射，而不是让流水凭空消失。</p>
     *
     * @param row 上游行
     * @return 模型编码
     */
    private String fallbackProductCode(GatewayPullRow row)
    {
        if (StringUtils.isEmpty(row.getModelKey()))
        {
            return null;
        }
        String code = row.getModelKey().trim();
        if (StringUtils.isNotEmpty(row.getModelVariant()))
        {
            code = code + "-" + row.getModelVariant().replace('|', '-');
        }
        return GatewayPullSupport.truncate(code, MAX_PRODUCT_CODE);
    }

    /**
     * 上游行组装为本系统流水
     *
     * @param row 上游行
     * @param account 渠道账户
     * @param customerId 已解析的客户ID
     * @param productCode 已解析的模型编码
     * @param prefix 流水号前缀
     * @return 流水
     */
    private GatewayCallLog toCallLog(GatewayPullRow row, CrmSupplierAccount account, Long customerId,
                                     String productCode, String prefix)
    {
        GatewayCallLog item = new GatewayCallLog();
        item.setCallNo(GatewayPullSupport.truncate(prefix + row.getUpstreamId(), 128));
        item.setCustomerId(customerId);
        item.setSupplierId(account.getSupplierId());
        item.setSupplierAccountId(account.getId());
        item.setProductCode(productCode);
        item.setModelType(row.getModelType());
        item.setPromptTokens(row.getPromptTokens());
        item.setCompletionTokens(row.getCompletionTokens());
        item.setTotalTokens(row.getTotalTokens());
        item.setTotalFees(row.getTotalFees());
        item.setActualFees(row.getActualFees());
        item.setCallTime(row.getCallTime());
        item.setCallStatus(row.getCallStatus());
        item.setErrMsg(GatewayPullSupport.truncate(row.getErrMsg(), MAX_ERR_MSG));
        item.setRawPriceDetail(row.getRawPriceDetail());
        return item;
    }

    /**
     * 校验必填字段：流水号、客户、渠道、模型编码、调用时间；渠道账户选填，缺省记为 0（未归属）
     *
     * @param item 流水
     * @return true 合法
     */
    private boolean isValid(GatewayCallLog item)
    {
        return item != null && StringUtils.isNotEmpty(item.getCallNo())
                && item.getCustomerId() != null && item.getSupplierId() != null
                && StringUtils.isNotEmpty(item.getProductCode()) && item.getCallTime() != null;
    }

    /**
     * 补齐缺省字段：token 汇总、调用状态、入库时间、删除标记
     *
     * @param item 流水
     */
    private void normalize(GatewayCallLog item)
    {
        item.setCallNo(item.getCallNo().trim());
        item.setProductCode(item.getProductCode().trim());
        if (item.getSupplierAccountId() == null || item.getSupplierAccountId() <= 0)
        {
            // 网关未上报账户时归入「未归属」，对账按渠道匹配，由匹配引擎判定是否可唯一确定
            item.setSupplierAccountId(0L);
        }
        if (item.getPromptTokens() == null)
        {
            item.setPromptTokens(0L);
        }
        if (item.getCompletionTokens() == null)
        {
            item.setCompletionTokens(0L);
        }
        if (item.getTotalTokens() == null || item.getTotalTokens() <= 0)
        {
            item.setTotalTokens(item.getPromptTokens() + item.getCompletionTokens());
        }
        if (item.getCallStatus() == null)
        {
            item.setCallStatus(1);
        }
        if (item.getCreateTime() == null)
        {
            item.setCreateTime(new Date());
        }
        item.setDeleted(0);
    }
}
