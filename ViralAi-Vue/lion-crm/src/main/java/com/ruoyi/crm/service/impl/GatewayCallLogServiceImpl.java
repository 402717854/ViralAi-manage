package com.ruoyi.crm.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.ruoyi.crm.dto.CrmCustomerSupplierAccountVo;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.dto.GatewayCallLogUploadDto;
import com.ruoyi.crm.mapper.CrmContractProductMapper;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
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

    /** gateway_call_log.call_no 长度 */
    private static final int MAX_CALL_NO = 128;

    /** 单次上传的最大数据行数，防止误传超大文件拖垮数据库 */
    private static final int MAX_UPLOAD_ROWS = 20000;

    /** 行级错误样例上限，避免一次上传把几千条错误塞进响应 */
    private static final int MAX_ROW_ERROR_SAMPLE = 20;

    /** 未映射用户名样例上限，避免一次拉取把几万个用户名塞进响应 */
    private static final int MAX_UNMAPPED_SAMPLE = 20;

    /** 流水拉取方式：2 文件上传（人工上传流水文件，不参与路由拉取） */
    private static final Integer PULL_TYPE_UPLOAD = 2;

    @Autowired
    private GatewayCallLogMapper gatewayCallLogMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private CrmContractProductMapper crmContractProductMapper;

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
                    + "文件上传方式的账户请到「网关原始流水 - 上传流水」导入流水文件");
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
     * 手工上传的流水文件解析结果入库
     *
     * <p>与路由拉取共用同一套模型映射与入库逻辑，差别只有数据来源：
     * 客户由上传界面指定（一个文件归属一个客户，文件里不再带客户标识，
     * 避免多客户混表时归错账），模型取「模型编码」列（填了就直接用）
     * 或「模型标识 + 模型变体」（走映射）。模型解析不出来的行不入库，
     * 只计数并给出样例行号。</p>
     *
     * @param rows 解析后的文件行
     * @param supplierId 流水所属渠道
     * @param supplierAccountId 渠道账户，必填
     * @param customerId 流水归属客户
     * @param fileName 上传文件名
     * @param operator 上传人
     * @return 统计结果
     */
    @Override
    public Map<String, Object> uploadAndIngest(List<GatewayCallLogUploadDto> rows, Long supplierId,
                                               Long supplierAccountId, Long customerId, String fileName,
                                               String operator)
    {
        if (supplierId == null)
        {
            throw new ServiceException("请选择流水所属渠道");
        }
        if (customerId == null)
        {
            throw new ServiceException("请选择流水归属客户");
        }
        if (supplierAccountId == null)
        {
            throw new ServiceException("请选择渠道账户");
        }
        if (crmCustomerMapper.selectCrmCustomerById(customerId) == null)
        {
            throw new ServiceException("客户不存在（客户ID=" + customerId + "）");
        }
        if (rows == null || rows.isEmpty())
        {
            throw new ServiceException("文件没有可解析的数据行，请确认第一行为表头、数据从第二行开始，并使用导入模板的表头名称");
        }
        if (rows.size() > MAX_UPLOAD_ROWS)
        {
            throw new ServiceException("单次最多上传 " + MAX_UPLOAD_ROWS + " 行，当前 " + rows.size()
                    + " 行，请拆分文件后分批上传");
        }
        List<CrmSupplierAccount> accounts = crmSupplierAccountMapper.selectAccountsForPull(supplierId,
                supplierAccountId);
        if (accounts == null || accounts.isEmpty())
        {
            throw new ServiceException("渠道账户不存在（账户ID=" + supplierAccountId + "）");
        }
        if (!supplierId.equals(accounts.get(0).getSupplierId()))
        {
            throw new ServiceException("账户 " + supplierAccountId + " 不属于渠道 " + supplierId);
        }
        // 客户 → 渠道 → 账户 必须是该客户生效合同明细里的组合，避免流水挂到合同外的渠道账户上导致对账永远匹配不到
        List<CrmCustomerSupplierAccountVo> combos = crmContractProductMapper
                .selectSupplierAccountsByCustomer(customerId);
        if (combos == null || combos.isEmpty())
        {
            throw new ServiceException("该客户没有已签约的合同，无法确认流水的渠道账户；请先在合同中配置渠道与账户明细");
        }
        List<CrmCustomerSupplierAccountVo> channelCombos = combos.stream()
                .filter(item -> supplierId.equals(item.getSupplierId()))
                .collect(Collectors.toList());
        if (channelCombos.isEmpty())
        {
            String names = combos.stream().map(CrmCustomerSupplierAccountVo::getSupplierName)
                    .filter(StringUtils::isNotEmpty).distinct().limit(5).collect(Collectors.joining("、"));
            throw new ServiceException("该客户的合同明细里没有所选渠道，不能上传该渠道的流水"
                    + (StringUtils.isEmpty(names) ? "" : "；合同内渠道：" + names));
        }
        // 历史合同明细可能未绑定账户（supplier_account_id 为空），此时只要求账户属于该渠道
        boolean accountBlank = channelCombos.stream().anyMatch(item -> item.getSupplierAccountId() == null);
        boolean accountMatched = channelCombos.stream()
                .anyMatch(item -> supplierAccountId.equals(item.getSupplierAccountId()));
        if (!accountMatched && !accountBlank)
        {
            String names = channelCombos.stream().map(CrmCustomerSupplierAccountVo::getSupplierAccountName)
                    .filter(StringUtils::isNotEmpty).distinct().limit(5).collect(Collectors.joining("、"));
            throw new ServiceException("所选账户不在该客户于本渠道的合同明细中"
                    + (StringUtils.isEmpty(names) ? "" : "；合同内账户：" + names));
        }
        GatewayPullMapping mapping = gatewayPullMappingLoader.load(mappingAccounts(accounts, supplierId));
        String trace = uploadTrace(fileName, operator);

        List<GatewayCallLog> logs = new ArrayList<>();
        List<String> rowErrors = new ArrayList<>();
        Set<String> missingModels = new LinkedHashSet<>();
        int invalid = 0;
        int missingModel = 0;
        Date min = null;
        Date max = null;

        // 表头占第 1 行，数据从第 2 行开始，行号用于回报错误位置
        int rowNo = 1;
        for (GatewayCallLogUploadDto row : rows)
        {
            rowNo++;
            if (row == null)
            {
                continue;
            }
            String callNo = StringUtils.trim(row.getCallNo());
            if (StringUtils.isEmpty(callNo))
            {
                invalid++;
                addRowError(rowErrors, rowNo, "流水号为空");
                continue;
            }
            if (row.getCallTime() == null)
            {
                invalid++;
                addRowError(rowErrors, rowNo, "调用时间为空或格式不是 yyyy-MM-dd HH:mm:ss");
                continue;
            }
            String productCode = resolveUploadProductCode(mapping, supplierId, supplierAccountId, row);
            if (StringUtils.isEmpty(productCode))
            {
                missingModel++;
                if (missingModels.size() < MAX_UNMAPPED_SAMPLE)
                {
                    missingModels.add(rowNo + " 行:" + (StringUtils.isEmpty(row.getModelKey()) ? "<空>"
                            : row.getModelKey().trim()));
                }
                continue;
            }
            GatewayCallLog item = new GatewayCallLog();
            item.setCallNo(GatewayPullSupport.truncate(callNo, MAX_CALL_NO));
            item.setCustomerId(customerId);
            item.setSupplierId(supplierId);
            item.setSupplierAccountId(supplierAccountId);
            item.setProductCode(productCode);
            item.setModelType(StringUtils.trim(row.getModelType()));
            item.setPromptTokens(row.getPromptTokens());
            item.setCompletionTokens(row.getCompletionTokens());
            item.setTotalTokens(row.getTotalTokens());
            item.setTotalFees(row.getTotalFees());
            item.setActualFees(row.getActualFees());
            item.setCallTime(row.getCallTime());
            item.setCallStatus(parseCallStatus(row.getCallStatusText()));
            item.setErrMsg(GatewayPullSupport.truncate(StringUtils.trim(row.getErrMsg()), MAX_ERR_MSG));
            item.setRawPriceDetail(trace);
            if (min == null || item.getCallTime().before(min))
            {
                min = item.getCallTime();
            }
            if (max == null || item.getCallTime().after(max))
            {
                max = item.getCallTime();
            }
            logs.add(item);
        }

        Map<String, Object> result = ingest(logs);
        int ingestInvalid = result.get("invalid") == null ? 0 : (Integer) result.get("invalid");
        result.put("source", "upload");
        result.put("parsed", rows.size());
        // 文件行数与入库校验口径合并，保证「解析 = 入库 + 重复 + 无效 + 缺模型」
        result.put("invalid", invalid + ingestInvalid);
        result.put("missingModel", missingModel);
        result.put("missingModelSample", new ArrayList<>(missingModels));
        result.put("rowErrors", rowErrors);
        result.put("supplierId", supplierId);
        result.put("supplierAccountId", supplierAccountId);
        result.put("customerId", customerId);
        result.put("startTime", min);
        result.put("endTime", max);
        log.info("流水文件上传完成，文件={}，上传人={}，渠道={}，账户={}，客户={}，解析 {} 行，入库 {} 条 / 重复 {} 条 / "
                        + "无效 {} 条 / 缺模型编码 {} 条",
                fileName, operator, supplierId, supplierAccountId, customerId, rows.size(),
                result.get("inserted"), result.get("duplicated"), result.get("invalid"), missingModel);
        return result;
    }

    /**
     * 上传行解析模型编码：直接填了模型编码就用，否则走映射再退化为上游标识
     *
     * @param mapping 映射索引
     * @param supplierId 渠道ID
     * @param supplierAccountId 账户ID
     * @param row 文件行
     * @return 模型编码，无法确定返回 null
     */
    private String resolveUploadProductCode(GatewayPullMapping mapping, Long supplierId, Long supplierAccountId,
                                            GatewayCallLogUploadDto row)
    {
        if (StringUtils.isNotEmpty(row.getProductCode()))
        {
            return GatewayPullSupport.truncate(row.getProductCode().trim(), MAX_PRODUCT_CODE);
        }
        String code = mapping.resolveProductCode(supplierId, supplierAccountId, row.getModelKey(),
                row.getModelVariant());
        if (StringUtils.isNotEmpty(code))
        {
            return GatewayPullSupport.truncate(code, MAX_PRODUCT_CODE);
        }
        if (StringUtils.isEmpty(row.getModelKey()))
        {
            return null;
        }
        String fallback = row.getModelKey().trim();
        if (StringUtils.isNotEmpty(row.getModelVariant()))
        {
            fallback = fallback + "-" + row.getModelVariant().trim().replace('|', '-');
        }
        return GatewayPullSupport.truncate(fallback, MAX_PRODUCT_CODE);
    }

    /**
     * 调用状态归一：兼容 成功/失败、1/0、true/false，留空按成功处理
     *
     * @param text 文件中的状态文本
     * @return 0 失败 / 1 成功
     */
    private Integer parseCallStatus(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return 1;
        }
        String value = text.trim().toLowerCase();
        if ("0".equals(value) || "失败".equals(value) || "fail".equals(value) || "failed".equals(value)
                || "false".equals(value) || "n".equals(value) || "no".equals(value))
        {
            return 0;
        }
        return 1;
    }

    /**
     * 上传来源标记，写入 raw_price_detail 便于按流水追溯是谁在什么时候上传的
     *
     * @param fileName 文件名
     * @param operator 上传人
     * @return JSON 片段
     */
    private String uploadTrace(String fileName, String operator)
    {
        String file = StringUtils.isEmpty(fileName) ? "" : fileName.replace("\\", "/").replace("\"", "'");
        String user = StringUtils.isEmpty(operator) ? "" : operator.replace("\"", "'");
        String json = "{\"source\":\"manual-upload\",\"file\":\"" + file + "\",\"operator\":\"" + user
                + "\",\"uploadTime\":\"" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
                + "\"}";
        return GatewayPullSupport.truncate(json, MAX_ERR_MSG);
    }

    /**
     * 追加行级错误样例
     *
     * @param rowErrors 错误样例集合
     * @param rowNo 行号
     * @param reason 原因
     */
    private void addRowError(List<String> rowErrors, int rowNo, String reason)
    {
        if (rowErrors.size() < MAX_ROW_ERROR_SAMPLE)
        {
            rowErrors.add("第 " + rowNo + " 行：" + reason);
        }
    }

    /**
     * 映射加载所需的账户清单
     *
     * <p>渠道下没有任何启用账户时仍要能命中渠道级规则，因此塞一个只带 supplierId 的探针账户，
     * 让映射加载器按渠道维度取规则。</p>
     *
     * @param accounts 账户清单，可能为空
     * @param supplierId 渠道ID
     * @return 非空账户清单
     */
    private List<CrmSupplierAccount> mappingAccounts(List<CrmSupplierAccount> accounts, Long supplierId)
    {
        if (accounts != null && !accounts.isEmpty())
        {
            return accounts;
        }
        CrmSupplierAccount probe = new CrmSupplierAccount();
        probe.setSupplierId(supplierId);
        return Collections.singletonList(probe);
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
