package com.ruoyi.crm.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.domain.CrmContractUsage;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.dto.CrmMatchProductDto;
import com.ruoyi.crm.enums.UsageMatchStatusEnum;
import com.ruoyi.crm.mapper.CrmContractUsageMapper;

/**
 * 合同匹配与计费引擎
 *
 * <p><b>唯一性口径</b>：同一客户 + 同一渠道 + 同一账户 + 同一模型，同一时间只允许一份生效合同明细。
 * 匹配以「合同明细行」为粒度，候选多于一行即判定为脏数据，不做自动择一，直接标记
 * {@link UsageMatchStatusEnum#CONTRACT_CONFLICT} 并输出告警日志，由差异工作台人工处理。</p>
 *
 * <p>匹配步骤：</p>
 * <ol>
 *   <li>模型编码解析为模型产品（编码重复视为脏数据）；</li>
 *   <li>按客户 + 渠道 + 账户 + 模型 + 调用时间取生效合同明细候选
 *       （合同已签约、调用时间落在 create_time ~ expire_time，expire_time 为空视为长期）；</li>
 *   <li>候选 1 行 → 校验折扣与基准价，计算金额；候选 0 行 → 细分无合同/合同过期/账户不匹配/模型不在合同内；
 *       候选多行 → 合同冲突（脏数据）。</li>
 * </ol>
 *
 * <p>账户兼容：流水账户为 0/空（网关未上报）时按渠道匹配；若此时候选仍多于一行，
 * 说明无法确定计费账户，同样判为冲突而非猜测。合同明细账户为空（历史未归属）按通配处理。</p>
 *
 * <p>计费公式：{@code 金额 = 上游费用 * 合同折扣}，保留 8 位小数（四舍五入），
 * 毛利 = 应收 - 成本。折扣取自当前时点的快照，写入台账后不再重算。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component
public class CrmUsageMatchEngine
{
    private static final Logger log = LoggerFactory.getLogger(CrmUsageMatchEngine.class);

    /** 每千 token 计价 */
    private static final BigDecimal THOUSAND = new BigDecimal("1000");

    /** 金额小数位（与 decimal(20,8) 一致） */
    private static final int AMOUNT_SCALE = 8;

    /** 中间计算精度 */
    private static final int CALC_SCALE = 10;

    @Autowired
    private CrmContractUsageMapper crmContractUsageMapper;

    /**
     * 执行匹配并回填台账字段（不落库）
     *
     * @param usage 台账记录，需包含 customerId / supplierId / supplierAccountId / productCode / callTime / totalTokens
     * @return true 匹配成功
     */
    public boolean match(CrmContractUsage usage)
    {
        clearResult(usage);

        if (usage.getCustomerId() == null || usage.getSupplierId() == null
                || StringUtils.isEmpty(usage.getProductCode()) || usage.getCallTime() == null)
        {
            return fail(usage, UsageMatchStatusEnum.OTHER_ERROR, "流水缺少客户、渠道、模型编码或调用时间，无法匹配");
        }

        List<CrmModelProduct> products = crmContractUsageMapper.selectModelProductsByCode(usage.getProductCode());
        if (products.isEmpty())
        {
            return fail(usage, UsageMatchStatusEnum.OTHER_ERROR,
                    "模型编码未维护或已失效：" + usage.getProductCode());
        }
        if (products.size() > 1)
        {
            return fail(usage, UsageMatchStatusEnum.OTHER_ERROR,
                    "模型编码重复，请先清理 crm_model_product：" + usage.getProductCode());
        }
        CrmModelProduct product = products.get(0);
        usage.setProductId(product.getId());

        Long accountId = normalizeAccountId(usage.getSupplierAccountId());
        //获取模型+渠道+渠道账户+合同  对应一条行数据
        List<CrmMatchProductDto> candidates = crmContractUsageMapper.selectMatchContractProducts(
                usage.getCustomerId(), usage.getSupplierId(), accountId, product.getId());
        if (candidates.size() > 1)
        {
            // 同客户+渠道+账户+模型存在多行生效明细：脏数据，不自动择一
            return conflict(usage, candidates);
        }

        if (candidates.isEmpty())
        {
//            List<CrmContract> validContracts = crmContractUsageMapper.selectMatchContracts(
//                    usage.getCustomerId(), usage.getSupplierId(), usage.getCallTime());
//            if (validContracts.isEmpty())
//            {
//                return failWithoutContract(usage);
//            }
//            List<CrmMatchProductDto> sameProductOtherAccount = crmContractUsageMapper.selectMatchContractProducts(
//                    usage.getCustomerId(), usage.getSupplierId(), null, product.getId());
//            if (!sameProductOtherAccount.isEmpty() && accountId != null)
//            {
//                return fail(usage, UsageMatchStatusEnum.OTHER_ERROR,
//                        "流水账户与合同明细账户不一致，流水账户=" + accountId
//                                + "，合同明细账户=" + describeAccounts(sameProductOtherAccount));
//            }
            return fail(usage, UsageMatchStatusEnum.PRODUCT_NOT_IN_CONTRACT,
                    "该客户:"+usage.getCustomerId()+"生效合同的模型清单中不含 " + usage.getProductCode()
                            + "（渠道 " + usage.getSupplierId() + "，账户 " + accountLabel(accountId) + "）");
        }

        CrmMatchProductDto hit = candidates.get(0);
        if (hit.getSellDiscount() == null || hit.getCostDiscount() == null)
        {
            return fail(usage, UsageMatchStatusEnum.OTHER_ERROR,
                    "合同明细折扣缺失，无法计费，合同=" + hit.getContactNo());
        }
        // actual_fees 允许为空（历史流水未上报该口径），统一按 0 参与计算，避免空指针
        BigDecimal actualFees = usage.getActualFees() == null ? BigDecimal.ZERO : usage.getActualFees();
        BigDecimal sellAmount = actualFees;//售价
        BigDecimal costAmount =BigDecimal.ZERO;//成本价
        BigDecimal totalFees = usage.getTotalFees() == null ? BigDecimal.ZERO : usage.getTotalFees();
        if(totalFees.compareTo(BigDecimal.ZERO)>0&&sellAmount.compareTo(BigDecimal.ZERO)==0){
            sellAmount = totalFees
                    .multiply(hit.getSellDiscount())
                    .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
            usage.setActualFees(sellAmount);
        }
        if(actualFees.compareTo(BigDecimal.ZERO)>0&&totalFees.compareTo(BigDecimal.ZERO)==0){
            totalFees = actualFees
                    .divide(hit.getSellDiscount(),AMOUNT_SCALE,RoundingMode.HALF_UP);
            usage.setTotalFees(totalFees);
        }
        costAmount = totalFees
                .multiply(hit.getCostDiscount())
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        usage.setContractId(hit.getContractId());
        usage.setContractProductId(hit.getContractProductId());
        if (accountId == null)
        {
            // 流水未归属账户时按合同明细回填，保证台账/账单可按账户归集
            usage.setSupplierAccountId(hit.getSupplierAccountId() == null ? 0L : hit.getSupplierAccountId());
        }
        usage.setSellDiscount(hit.getSellDiscount());
        usage.setCostDiscount(hit.getCostDiscount());
        usage.setSellAmount(sellAmount);
        usage.setCostAmount(costAmount);
        usage.setGrossProfit(sellAmount.subtract(costAmount));
        usage.setMatchStatus(UsageMatchStatusEnum.SUCCESS.getCode());
        usage.setMatchMsg("匹配合同 " + hit.getContactNo() + "，模型 " + usage.getProductCode()
                + "，账户 " + accountLabel(accountId == null ? hit.getSupplierAccountId() : accountId));
        return true;
    }

    /**
     * 合同冲突（脏数据）：同客户+渠道+账户+模型存在多行生效明细
     *
     * <p>不做自动择一，标记异常并输出告警日志，同时由调用方落差异记录进入人工处理。</p>
     *
     * @param usage 台账
     * @param candidates 候选明细
     * @return false
     */
    private boolean conflict(CrmContractUsage usage, List<CrmMatchProductDto> candidates)
    {
        List<String> contactNos = new ArrayList<>();
        List<String> detailIds = new ArrayList<>();
        for (CrmMatchProductDto candidate : candidates)
        {
            if (candidate.getContactNo() != null && !contactNos.contains(candidate.getContactNo()))
            {
                contactNos.add(candidate.getContactNo());
            }
            detailIds.add(String.valueOf(candidate.getContractProductId()));
        }
        boolean accountMissing = normalizeAccountId(usage.getSupplierAccountId()) == null;
        String reason = accountMissing
                ? "流水未归属渠道账户，且同模型存在 " + candidates.size() + " 条候选明细，无法确定计费账户"
                : "存在 " + candidates.size() + " 条覆盖同一模型的生效合同明细（客户+渠道+账户+模型唯一性冲突）";
        log.error("对账脏数据告警：{}；customerId={}, supplierId={}, supplierAccountId={}, productCode={}, "
                        + "callNo={}, callTime={}, 合同={}, 明细={}",
                reason, usage.getCustomerId(), usage.getSupplierId(), usage.getSupplierAccountId(),
                usage.getProductCode(), usage.getCallNo(), usage.getCallTime(),
                String.join("、", contactNos), String.join("、", detailIds));
        return fail(usage, UsageMatchStatusEnum.CONTRACT_CONFLICT,
                reason + "，涉及合同：" + String.join("、", contactNos) + "，请人工处理");
    }

    /**
     * 未找到生效合同时细分原因：完全无合同 / 有合同但已过期或未生效
     *
     * @param usage 台账
     * @return false
     */
    private boolean failWithoutContract(CrmContractUsage usage)
    {
        List<CrmContract> related = crmContractUsageMapper.selectRelatedContracts(
                usage.getCustomerId(), usage.getSupplierId());
        if (related.isEmpty())
        {
            return fail(usage, UsageMatchStatusEnum.NO_VALID_CONTRACT,
                    "客户与该渠道不存在任何合同");
        }
        List<String> signed = new ArrayList<>();
        for (CrmContract contract : related)
        {
            // 仅已签约合同的「过期」才有业务含义，草稿/审批中/作废合同视为无生效合同
            if (contract.getStatus() != null && contract.getStatus() == 2)
            {
                signed.add(contract.getContactNo());
            }
        }
        if (!signed.isEmpty())
        {
            return fail(usage, UsageMatchStatusEnum.CONTRACT_EXPIRED,
                    "调用时间不在合同有效期内，合同：" + String.join("、", signed));
        }
        return fail(usage, UsageMatchStatusEnum.NO_VALID_CONTRACT,
                "客户与该渠道仅有草稿/审批中/作废合同，无已签约合同");
    }

    /**
     * 清空上一次匹配结果，避免重复匹配残留旧值
     *
     * @param usage 台账
     */
    private void clearResult(CrmContractUsage usage)
    {
        usage.setContractId(null);
        usage.setContractProductId(null);
        usage.setProductId(null);
        usage.setBaseSellPrice(null);
        usage.setSellDiscount(null);
        usage.setSellAmount(null);
        usage.setBaseCostPrice(null);
        usage.setCostDiscount(null);
        usage.setCostAmount(null);
        usage.setGrossProfit(null);
    }

    /**
     * 标记匹配失败
     *
     * @param usage 台账
     * @param status 匹配状态
     * @param msg 说明
     * @return false
     */
    private boolean fail(CrmContractUsage usage, UsageMatchStatusEnum status, String msg)
    {
        usage.setMatchStatus(status.getCode());
        usage.setMatchMsg(StringUtils.substring(msg, 0, 250));
        return false;
    }

    /**
     * 账户ID归一：null 或非正数视为「未归属」
     *
     * @param accountId 账户ID
     * @return 归一后的账户ID，未归属返回 null
     */
    private Long normalizeAccountId(Long accountId)
    {
        return accountId == null || accountId <= 0 ? null : accountId;
    }

    /**
     * 账户展示文案
     *
     * @param accountId 账户ID
     * @return 文案
     */
    private String accountLabel(Long accountId)
    {
        return accountId == null ? "未归属" : String.valueOf(accountId);
    }

    /**
     * 候选明细的账户集合文案（去重）
     *
     * @param candidates 候选明细
     * @return 文案
     */
    private String describeAccounts(List<CrmMatchProductDto> candidates)
    {
        List<String> accounts = new ArrayList<>();
        for (CrmMatchProductDto candidate : candidates)
        {
            String label = accountLabel(candidate.getSupplierAccountId());
            if (!accounts.contains(label))
            {
                accounts.add(label);
            }
        }
        return String.join("、", accounts);
    }

    /**
     * 以流水构造台账初始记录（未匹配状态）
     *
     * @param callLogId 流水ID
     * @param callNo 流水号
     * @param customerId 客户ID
     * @param supplierId 渠道ID
     * @param supplierAccountId 渠道账户ID
     * @param productCode 模型编码
     * @param totalTokens token
     * @param totalFees 总消耗费用
     * @param actualFees 实际消耗费用
     * @param callTime 调用时间
     * @return 台账
     */
    public CrmContractUsage buildFromCallLog(Long callLogId, String callNo, Long customerId, Long supplierId,
                                             Long supplierAccountId, String productCode, Long totalTokens,
                                             BigDecimal totalFees,BigDecimal actualFees,Date callTime)
    {
        CrmContractUsage usage = new CrmContractUsage();
        usage.setCallLogId(callLogId);
        usage.setCallNo(callNo);
        usage.setCustomerId(customerId);
        usage.setSupplierId(supplierId);
        usage.setSupplierAccountId(supplierAccountId == null ? 0L : supplierAccountId);
        usage.setProductCode(productCode);
        usage.setTotalTokens(totalTokens == null ? 0L : totalTokens);
        usage.setTotalFees(totalFees == null ? BigDecimal.ZERO : totalFees);
        usage.setActualFees(actualFees == null ? BigDecimal.ZERO : actualFees);
        usage.setSellAmount(actualFees == null ? BigDecimal.ZERO : actualFees);
        usage.setCallTime(callTime);
        usage.setMatchStatus(UsageMatchStatusEnum.PENDING.getCode());
        usage.setUsageType(0);
        return usage;
    }
}
