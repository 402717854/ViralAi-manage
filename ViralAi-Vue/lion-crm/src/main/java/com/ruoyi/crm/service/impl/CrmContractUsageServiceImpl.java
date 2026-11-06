package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmContractBillDiff;
import com.ruoyi.crm.domain.CrmContractUsage;
import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.CrmSupplierStatVo;
import com.ruoyi.crm.dto.CrmUsageBoardVo;
import com.ruoyi.crm.dto.CrmUsageReconcileResultVo;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;
import com.ruoyi.crm.enums.BillDiffHandleStatusEnum;
import com.ruoyi.crm.enums.BillDiffTypeEnum;
import com.ruoyi.crm.enums.UsageMatchStatusEnum;
import com.ruoyi.crm.mapper.CrmContractBillDiffMapper;
import com.ruoyi.crm.mapper.CrmContractBillMapper;
import com.ruoyi.crm.mapper.CrmContractUsageMapper;
import com.ruoyi.crm.mapper.GatewayCallLogMapper;
import com.ruoyi.crm.service.ICrmContractUsageService;
import com.ruoyi.crm.service.support.CrmBillCycleUtils;
import com.ruoyi.crm.service.support.CrmFinanceDataScope;
import com.ruoyi.crm.service.support.CrmUsageMatchEngine;

/**
 * 合同用量对账Service实现
 *
 * <p>对账主流程（T+1 或手工触发）：</p>
 * <ol>
 *   <li>扫描区间内「调用成功且尚无台账」的网关流水，逐条生成台账并立即匹配；</li>
 *   <li>扫描区间内未生成账单的待匹配/异常台账，重新匹配（补签合同后可用）；</li>
 *   <li>匹配失败的一律落差异记录，进入差异处理工作台。</li>
 * </ol>
 *
 * <p>幂等：台账对 {@code call_log_id} 唯一，流水对 {@code call_no} 唯一，差异对 {@code usage_id} 唯一，
 * 重复执行不会产生重复计费或重复差异。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmContractUsageServiceImpl implements ICrmContractUsageService
{
    /** 合同维度排行取前 N 条 */
    private static final int RANK_LIMIT = 10;

    @Autowired
    private CrmContractUsageMapper crmContractUsageMapper;

    @Autowired
    private GatewayCallLogMapper gatewayCallLogMapper;

    @Autowired
    private CrmContractBillMapper crmContractBillMapper;

    @Autowired
    private CrmContractBillDiffMapper crmContractBillDiffMapper;

    @Autowired
    private CrmUsageMatchEngine crmUsageMatchEngine;

    @Autowired
    private CrmFinanceDataScope crmFinanceDataScope;

    @Override
    public List<CrmContractUsageDto> selectCrmContractUsageList(CrmContractUsageDto query)
    {
        crmFinanceDataScope.applyScope(query);
        return crmContractUsageMapper.selectCrmContractUsageList(query);
    }

    @Override
    public CrmContractUsageDto selectCrmContractUsageById(Long id)
    {
        CrmContractUsageDto usage = crmContractUsageMapper.selectCrmContractUsageById(id);
        if (usage == null)
        {
            throw new ServiceException("用量记录不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(usage.getSupplierId());
        return usage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmUsageReconcileResultVo reconcile(CrmUsageReconcileVo vo)
    {
        Date start = CrmBillCycleUtils.parseStart(vo == null ? null : vo.getStartTime());
        Date end = CrmBillCycleUtils.parseEnd(vo == null ? null : vo.getEndTime());
        if (start.after(end))
        {
            throw new ServiceException("对账起始时间不能晚于结束时间");
        }
        Long supplierId = vo == null ? null : vo.getSupplierId();
        Boolean retryAbnormal = vo == null ? null : vo.getRetryAbnormal();
        if (supplierId != null)
        {
            crmFinanceDataScope.assertSupplierAccessible(supplierId);
        }

        // 渠道范围：指定渠道 > 当前账号可见渠道 > 全量（定时任务）
        List<Long> scope = crmFinanceDataScope.scopeSupplierIds();
        List<Long> targets;
        if (supplierId != null)
        {
            targets = Collections.singletonList(supplierId);
        }
        else if (scope != null)
        {
            targets = scope;
        }
        else
        {
            targets = Collections.singletonList(null);
        }

        CrmUsageReconcileResultVo result = new CrmUsageReconcileResultVo();
        result.setStartTime(start);
        result.setEndTime(end);
        result.setScanned(0);
        result.setCreated(0);
        result.setRematched(0);
        result.setMatched(0);
        result.setAbnormal(0);
        result.setDiffCreated(0);

        for (Long target : targets)
        {
            reconcileCallLogs(target, start, end, result);
//            retryUsages(target, start, end, retryAbnormal, scope, result);
        }
        return result;
    }

    /**
     * 新流水生成本台账并立即匹配
     *
     * @param supplierId 渠道ID，为空表示不限
     * @param start 起始
     * @param end 结束
     * @param result 结果累计
     */
    private void reconcileCallLogs(Long supplierId, Date start, Date end, CrmUsageReconcileResultVo result)
    {
        List<GatewayCallLog> logs = gatewayCallLogMapper.selectReconcileCallLogs(start, end, supplierId);
        for (GatewayCallLog callLog : logs)
        {
            result.setScanned(result.getScanned() + 1);
            CrmContractUsage usage = crmUsageMatchEngine.buildFromCallLog(callLog.getId(), callLog.getCallNo(),
                    callLog.getCustomerId(), callLog.getSupplierId(), callLog.getSupplierAccountId(),
                    callLog.getProductCode(), callLog.getTotalTokens(),callLog.getTotalFees(), callLog.getCallTime());
            usage.setCreateTime(new Date());
            try
            {
                crmContractUsageMapper.insertCrmContractUsage(usage);
            }
            catch (DataIntegrityViolationException e)
            {
                // 并发下已被其他任务处理，直接跳过
                continue;
            }
            result.setCreated(result.getCreated() + 1);

            boolean matched = crmUsageMatchEngine.match(usage);
            crmContractUsageMapper.updateCrmContractUsage(usage);
            if (matched)
            {
                result.setMatched(result.getMatched() + 1);
            }
            else
            {
                result.setAbnormal(result.getAbnormal() + 1);
                if (createDiff(usage))
                {
                    result.setDiffCreated(result.getDiffCreated() + 1);
                }
            }
        }
    }

    /**
     * 未匹配（或需重试）台账重新匹配
     *
     * @param supplierId 渠道ID，为空表示不限
     * @param start 起始
     * @param end 结束
     * @param retryAbnormal 是否包含异常记录
     * @param scope 数据权限渠道集合
     * @param result 结果累计
     */
    private void retryUsages(Long supplierId, Date start, Date end, Boolean retryAbnormal,
                             List<Long> scope, CrmUsageReconcileResultVo result)
    {
        List<Integer> statuses;
        if (Boolean.FALSE.equals(retryAbnormal))
        {
            statuses = Collections.singletonList(UsageMatchStatusEnum.PENDING.getCode());
        }
        else
        {
            statuses = Arrays.asList(
                    UsageMatchStatusEnum.PENDING.getCode(),
                    UsageMatchStatusEnum.NO_VALID_CONTRACT.getCode(),
                    UsageMatchStatusEnum.CONTRACT_EXPIRED.getCode(),
                    UsageMatchStatusEnum.PRODUCT_NOT_IN_CONTRACT.getCode(),
                    UsageMatchStatusEnum.SUPPLIER_MISMATCH.getCode(),
                    UsageMatchStatusEnum.OTHER_ERROR.getCode(),
                    UsageMatchStatusEnum.CONTRACT_CONFLICT.getCode());
        }
        List<CrmContractUsage> usages = crmContractUsageMapper.selectRematchUsages(start, end, supplierId, statuses, scope);
        for (CrmContractUsage usage : usages)
        {
            result.setScanned(result.getScanned() + 1);
            result.setRematched(result.getRematched() + 1);
            boolean matched = crmUsageMatchEngine.match(usage);
            crmContractUsageMapper.updateCrmContractUsage(usage);
            if (matched)
            {
                result.setMatched(result.getMatched() + 1);
                closeDiffIfMatched(usage.getId());
            }
            else
            {
                result.setAbnormal(result.getAbnormal() + 1);
                if (createDiff(usage))
                {
                    result.setDiffCreated(result.getDiffCreated() + 1);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rematch(Long usageId)
    {
        if (usageId == null)
        {
            throw new ServiceException("用量ID不能为空");
        }
        CrmContractUsage usage = crmContractUsageMapper.selectCrmContractUsageEntity(usageId);
        if (usage == null)
        {
            throw new ServiceException("用量记录不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(usage.getSupplierId());
        if (usage.getBillId() != null)
        {
            throw new ServiceException("该用量已生成账单，账单口径不可变更；如需调整请走差异调账");
        }
        if (usage.getUsageType() != null && usage.getUsageType() == 1)
        {
            throw new ServiceException("调账冲销记录不支持重新匹配");
        }

        boolean matched = crmUsageMatchEngine.match(usage);
        crmContractUsageMapper.updateCrmContractUsage(usage);
        if (matched)
        {
            closeDiffIfMatched(usageId);
            return 1;
        }
        createDiff(usage);
        return 0;
    }

    @Override
    public CrmUsageBoardVo board(String billCycle)
    {
        String cycle = CrmBillCycleUtils.normalize(billCycle);
        Date[] range = CrmBillCycleUtils.rangeOf(cycle);
        List<Long> scope = crmFinanceDataScope.scopeSupplierIds();

        CrmUsageBoardVo board = crmContractUsageMapper.selectBoardSummary(range[0], range[1], scope);
        if (board == null)
        {
            board = new CrmUsageBoardVo();
        }
        board.setBillCycle(cycle);

        List<CrmSupplierStatVo> supplierStats = crmContractUsageMapper.selectSupplierStat(range[0], range[1], scope);
        for (CrmSupplierStatVo stat : supplierStats)
        {
            stat.setProfitRate(rate(stat.getTotalGrossProfit(), stat.getTotalSellAmount()));
            BigDecimal calls = stat.getTotalCalls() == null ? BigDecimal.ZERO : BigDecimal.valueOf(stat.getTotalCalls());
            BigDecimal abnormal = stat.getAbnormalCount() == null ? BigDecimal.ZERO
                    : BigDecimal.valueOf(stat.getAbnormalCount());
            stat.setAbnormalRate(rate(abnormal, calls));
        }
        board.setSupplierStats(supplierStats);
        board.setContractRank(crmContractUsageMapper.selectContractRank(range[0], range[1], scope, RANK_LIMIT));
        board.setProfitRate(rate(board.getTotalGrossProfit(), board.getTotalSellAmount()));

        BigDecimal calls = board.getTotalCalls() == null ? BigDecimal.ZERO : BigDecimal.valueOf(board.getTotalCalls());
        BigDecimal abnormal = board.getAbnormalCount() == null ? BigDecimal.ZERO
                : BigDecimal.valueOf(board.getAbnormalCount());
        board.setAbnormalRate(rate(abnormal, calls));
        board.setBillCount(crmContractBillMapper.countBillsByCycle(cycle, scope));
        board.setPendingDiffCount(crmContractBillDiffMapper.countPending(scope, range[0], range[1]));
        return board;
    }

    @Override
    public List<CrmSupplierStatVo> selectSupplierStat(String billCycle)
    {
        String cycle = CrmBillCycleUtils.normalize(billCycle);
        Date[] range = CrmBillCycleUtils.rangeOf(cycle);
        List<Long> scope = crmFinanceDataScope.scopeSupplierIds();
        List<CrmSupplierStatVo> list = crmContractUsageMapper.selectSupplierStat(range[0], range[1], scope);
        for (CrmSupplierStatVo stat : list)
        {
            stat.setProfitRate(rate(stat.getTotalGrossProfit(), stat.getTotalSellAmount()));
            BigDecimal calls = stat.getTotalCalls() == null ? BigDecimal.ZERO : BigDecimal.valueOf(stat.getTotalCalls());
            BigDecimal abnormal = stat.getAbnormalCount() == null ? BigDecimal.ZERO
                    : BigDecimal.valueOf(stat.getAbnormalCount());
            stat.setAbnormalRate(rate(abnormal, calls));
        }
        return list;
    }

    /**
     * 匹配失败时生成差异记录（同一用量只保留一条）
     *
     * @param usage 台账
     * @return true 新建成功
     */
    private boolean createDiff(CrmContractUsage usage)
    {
        if (usage.getId() == null || usage.getBillId() != null)
        {
            return false;
        }
        BillDiffTypeEnum type = BillDiffTypeEnum.fromMatchStatus(usage.getMatchStatus());
        if (type == null)
        {
            return false;
        }
        CrmContractBillDiff diff = new CrmContractBillDiff();
        diff.setUsageId(usage.getId());
        diff.setDiffType(type.getCode());
        diff.setDiffDesc(StringUtils.substring(
                type.getName() + "：" + (usage.getMatchMsg() == null ? "匹配失败" : usage.getMatchMsg()), 0, 480));
        diff.setSupplierId(usage.getSupplierId());
        diff.setCustomerId(usage.getCustomerId());
        diff.setCallNo(usage.getCallNo());
        diff.setCallTime(usage.getCallTime());
        diff.setAmount(usage.getSellAmount());
        diff.setHandleStatus(BillDiffHandleStatusEnum.PENDING.getCode());
        diff.setCreateTime(new Date());
        try
        {
            crmContractBillDiffMapper.insertCrmContractBillDiff(diff);
            return true;
        }
        catch (DataIntegrityViolationException e)
        {
            // uk_usage_id 冲突：已存在该用量的差异记录
            return false;
        }
    }

    /**
     * 重新匹配成功后自动关闭待处理差异
     *
     * @param usageId 台账ID
     */
    private void closeDiffIfMatched(Long usageId)
    {
        CrmContractBillDiffDto query = new CrmContractBillDiffDto();
        query.setUsageId(usageId);
        List<CrmContractBillDiffDto> diffs = crmContractBillDiffMapper.selectCrmContractBillDiffList(query);
        for (CrmContractBillDiffDto diff : diffs)
        {
            if (BillDiffHandleStatusEnum.PENDING.getCode().equals(diff.getHandleStatus()))
            {
                crmContractBillDiffMapper.updateDiffHandle(diff.getId(),
                        BillDiffHandleStatusEnum.ADJUSTED.getCode(),
                        "重新匹配成功，已按新合同计费，差异自动关闭",
                        null, new Date());
            }
        }
    }

    /**
     * 计算百分比
     *
     * @param part 分子
     * @param total 分母
     * @return 百分比，保留 2 位
     */
    private BigDecimal rate(BigDecimal part, BigDecimal total)
    {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0)
        {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return part.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }
}
