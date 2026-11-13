package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.domain.CrmContractBill;
import com.ruoyi.crm.domain.CrmContractBillDiff;
import com.ruoyi.crm.domain.CrmContractUsage;
import com.ruoyi.crm.dto.CrmBillAuditVo;
import com.ruoyi.crm.dto.CrmBillDiffHandleVo;
import com.ruoyi.crm.dto.CrmBillGenerateResultVo;
import com.ruoyi.crm.dto.CrmBillGenerateVo;
import com.ruoyi.crm.dto.CrmBillGroupDto;
import com.ruoyi.crm.dto.CrmContractBillDetailVo;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;
import com.ruoyi.crm.dto.CrmContractBillDto;
import com.ruoyi.crm.enums.BillDiffHandleStatusEnum;
import com.ruoyi.crm.enums.ContractBillStatusEnum;
import com.ruoyi.crm.enums.UsageMatchStatusEnum;
import com.ruoyi.crm.mapper.CrmBusinessMapper;
import com.ruoyi.crm.mapper.CrmContractBillDiffMapper;
import com.ruoyi.crm.mapper.CrmContractBillMapper;
import com.ruoyi.crm.mapper.CrmContractMapper;
import com.ruoyi.crm.mapper.CrmContractUsageMapper;
import com.ruoyi.crm.service.ICrmContractBillService;
import com.ruoyi.crm.service.support.CrmBillCycleUtils;
import com.ruoyi.crm.service.support.CrmBillNoGenerator;
import com.ruoyi.crm.service.support.CrmFinanceDataScope;

/**
 * 对账账单Service实现
 *
 * <p>账单生成后，归属用量回填 {@code bill_id} 即被锁定：后续对账不再重算、
 * 重新匹配也会跳过（需求文档 5.1「账单生成后 usage 只读」）。</p>
 *
 * <p>状态流转：生成 → 待复核 → 复核确认 → 已结算；待复核阶段若存在未处理差异，
 * 可直接标记「差异挂起」，差异处理完毕后重新复核。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmContractBillServiceImpl implements ICrmContractBillService
{
    /** 系统任务生成账单时的默认创建人（admin），避免无登录上下文写入 null */
    private static final Long SYSTEM_USER_ID = 1L;

    @Autowired
    private CrmContractBillMapper crmContractBillMapper;

    @Autowired
    private CrmContractUsageMapper crmContractUsageMapper;

    @Autowired
    private CrmContractBillDiffMapper crmContractBillDiffMapper;

    @Autowired
    private CrmContractMapper crmContractMapper;

    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

    @Autowired
    private CrmBillNoGenerator crmBillNoGenerator;

    @Autowired
    private CrmFinanceDataScope crmFinanceDataScope;

    @Override
    public List<CrmContractBillDto> selectCrmContractBillList(CrmContractBillDto query)
    {
        crmFinanceDataScope.applyScope(query);
        return crmContractBillMapper.selectCrmContractBillList(query);
    }

    @Override
    public CrmContractBillDetailVo selectCrmContractBillDetail(Long id)
    {
        CrmContractBillDto bill = crmContractBillMapper.selectCrmContractBillDetail(id);
        if (bill == null)
        {
            throw new ServiceException("账单不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(bill.getSupplierId());

        CrmContractBillDetailVo detail = new CrmContractBillDetailVo();
        detail.setBill(bill);
        detail.setUsages(crmContractUsageMapper.selectUsageByBillId(id));

        CrmContractBillDiffDto diffQuery = new CrmContractBillDiffDto();
        diffQuery.setSupplierId(bill.getSupplierId());
        diffQuery.getParams().put("beginTime", formatDateTime(bill.getStartTime()));
        diffQuery.getParams().put("endTime", formatDateTime(bill.getEndTime()));
        List<CrmContractBillDiffDto> diffs = crmContractBillDiffMapper.selectCrmContractBillDiffList(diffQuery);
        detail.setDiffs(diffs);

        int pending = 0;
        for (CrmContractBillDiffDto diff : diffs)
        {
            if (BillDiffHandleStatusEnum.PENDING.getCode().equals(diff.getHandleStatus()))
            {
                pending++;
            }
        }
        detail.setPendingDiffCount(pending);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmBillGenerateResultVo generate(CrmBillGenerateVo vo)
    {
        if (vo == null || vo.getSupplierId() == null)
        {
            throw new ServiceException("渠道不能为空");
        }
        String cycle = CrmBillCycleUtils.normalize(vo.getBillCycle());
        crmFinanceDataScope.assertSupplierAccessible(vo.getSupplierId());
        return generateForSupplier(vo.getSupplierId(), cycle, vo.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(CrmBillAuditVo vo)
    {
        if (vo == null || vo.getBillId() == null)
        {
            throw new ServiceException("账单ID不能为空");
        }
        Integer target = vo.getBillStatus();
        if (!ContractBillStatusEnum.CONFIRMED.getCode().equals(target)
                && !ContractBillStatusEnum.DIFF_HANG.getCode().equals(target))
        {
            throw new ServiceException("复核结果只支持「已确认」或「差异挂起」");
        }
        CrmContractBill bill = crmContractBillMapper.selectCrmContractBillById(vo.getBillId());
        if (bill == null)
        {
            throw new ServiceException("账单不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(bill.getSupplierId());

        Integer current = bill.getBillStatus();
        if (ContractBillStatusEnum.SETTLED.getCode().equals(current))
        {
            throw new ServiceException("已结算的账单不可再复核");
        }
        if (ContractBillStatusEnum.CONFIRMED.getCode().equals(current)
                && ContractBillStatusEnum.CONFIRMED.getCode().equals(target))
        {
            throw new ServiceException("账单已确认，无需重复复核");
        }
        if (ContractBillStatusEnum.CONFIRMED.getCode().equals(target))
        {
            int pending = crmContractBillDiffMapper.countPending(
                    Collections.singletonList(bill.getSupplierId()), bill.getStartTime(), bill.getEndTime());
            if (pending > 0)
            {
                throw new ServiceException("该账期还有 " + pending + " 条待处理差异，请先处理差异或标记为差异挂起");
            }
        }
        String remark = StringUtils.isEmpty(vo.getRemark()) ? bill.getRemark() : vo.getRemark();
        return crmContractBillMapper.updateBillAudit(bill.getId(), target,
                StringUtils.substring(remark, 0, 480), currentUserId(), new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int settle(Long billId)
    {
        if (billId == null)
        {
            throw new ServiceException("账单ID不能为空");
        }
        CrmContractBill bill = crmContractBillMapper.selectCrmContractBillById(billId);
        if (bill == null)
        {
            throw new ServiceException("账单不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(bill.getSupplierId());
        if (!ContractBillStatusEnum.CONFIRMED.getCode().equals(bill.getBillStatus()))
        {
            throw new ServiceException("仅「已确认」的账单可以结算");
        }
        int rows = crmContractBillMapper.updateBillAudit(billId, ContractBillStatusEnum.SETTLED.getCode(),
                bill.getRemark(), currentUserId(), new Date());
        writeBackBusinessSettle(bill.getContractId());
        return rows;
    }

    @Override
    public List<CrmContractBillDiffDto> selectDiffList(CrmContractBillDiffDto query)
    {
        crmFinanceDataScope.applyScope(query);
        return crmContractBillDiffMapper.selectCrmContractBillDiffList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int handleDiff(CrmBillDiffHandleVo vo)
    {
        if (vo == null || vo.getDiffId() == null)
        {
            throw new ServiceException("差异ID不能为空");
        }
        BillDiffHandleStatusEnum action = BillDiffHandleStatusEnum.getByCode(vo.getHandleStatus());
        if (action == null || BillDiffHandleStatusEnum.PENDING.equals(action))
        {
            throw new ServiceException("处理方式只支持「调账」「豁免」「驳回」");
        }
        CrmContractBillDiff diff = crmContractBillDiffMapper.selectCrmContractBillDiffEntity(vo.getDiffId());
        if (diff == null)
        {
            throw new ServiceException("差异记录不存在");
        }
        crmFinanceDataScope.assertSupplierAccessible(diff.getSupplierId());
        if (!BillDiffHandleStatusEnum.PENDING.getCode().equals(diff.getHandleStatus()))
        {
            throw new ServiceException("该差异已处理，无需重复操作");
        }
        if (BillDiffHandleStatusEnum.ADJUSTED.equals(action))
        {
            createReverseUsage(diff, vo);
        }
        return crmContractBillDiffMapper.updateDiffHandle(diff.getId(), action.getCode(),
                StringUtils.substring(vo.getHandleRemark(), 0, 480), currentUserId(), new Date());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateMonthlyBills(String billCycle)
    {
        String cycle = CrmBillCycleUtils.normalize(billCycle);
        Date[] range = CrmBillCycleUtils.rangeOf(cycle);
        List<Long> supplierIds = crmContractUsageMapper.selectSupplierIdsWithUsage(range[0], range[1]);
        int count = 0;
        for (Long supplierId : supplierIds)
        {
            CrmBillGenerateResultVo result = generateForSupplier(supplierId, cycle, "系统按账期自动生成");
            count += result.getBillCount();
        }
        return count;
    }

    /**
     * 生成指定渠道指定账期的账单
     *
     * <p>口径 B（已确认）：同一合同同账期允许多张账单。已存在账单时本次生成「补充账单」，
     * 只归集尚未入账的用量，账单唯一性由 {@code bill_no} 保证，库里不建
     * {@code (contract_id, supplier_account_id, bill_cycle)} 唯一键。</p>
     *
     * <p>账单按「合同 + 渠道 + 账期」跨账户汇总，{@code supplier_account_id} 固定写 0；
     * 账户维度只作为用量明细的归集维度，不改变出账粒度。</p>
     *
     * @param supplierId 渠道ID
     * @param billCycle 账期
     * @param remark 备注
     * @return 生成结果
     */
    private CrmBillGenerateResultVo generateForSupplier(Long supplierId, String billCycle, String remark)
    {
        Date[] range = CrmBillCycleUtils.rangeOf(billCycle);
        List<CrmBillGroupDto> groups = crmContractUsageMapper.selectBillGroups(supplierId, range[0], range[1]);

        CrmBillGenerateResultVo result = new CrmBillGenerateResultVo();
        result.setSupplierId(supplierId);
        result.setBillCycle(billCycle);
        result.setBillCount(0);
        result.setUsageCount(0);
        result.setSkippedCount(0);
        result.setTotalSellAmount(BigDecimal.ZERO);
        result.setTotalCostAmount(BigDecimal.ZERO);
        result.setTotalGrossProfit(BigDecimal.ZERO);
        result.setBills(new ArrayList<>());

        Long creator = currentUserId();
        for (CrmBillGroupDto group : groups)
        {
            // 同合同同账期已存在账单时，本次生成的是「补充账单」：只归集尚未入账的用量，
            // 不影响已生成账单的金额与状态（补签合同、网关补推等场景）
            int existBills = crmContractBillMapper.countByContractAndCycle(
                    group.getContractId(), supplierId, billCycle);

            CrmContractBill bill = new CrmContractBill();
            bill.setBillNo(crmBillNoGenerator.generate(supplierId, billCycle));
            bill.setContractId(group.getContractId());
            bill.setCustomerId(group.getCustomerId());
            bill.setSupplierId(supplierId);
            bill.setBillCycle(billCycle);
            bill.setStartTime(range[0]);
            bill.setEndTime(range[1]);
            bill.setTotalTokens(group.getTotalTokens() == null ? 0L : group.getTotalTokens());
            bill.setTotalSellAmount(nullToZero(group.getTotalSellAmount()));
            bill.setTotalCostAmount(nullToZero(group.getTotalCostAmount()));
            bill.setTotalGrossProfit(nullToZero(group.getTotalGrossProfit()));
            bill.setBillStatus(ContractBillStatusEnum.WAIT_AUDIT.getCode());
            String billRemark = existBills > 0 ? "补充账单（同账期已有 " + existBills + " 张）" : "";
            if (StringUtils.isNotEmpty(remark))
            {
                billRemark = StringUtils.isEmpty(billRemark) ? remark : billRemark + "；" + remark;
            }
            bill.setRemark(StringUtils.substring(billRemark, 0, 480));
            bill.setCreator(creator);
            bill.setCreateTime(new Date());
            crmContractBillMapper.insertCrmContractBill(bill);

            int usageCount = crmContractUsageMapper.updateBillIdByContract(bill.getId(), group.getContractId(),
                    supplierId, range[0], range[1]);
            if (usageCount == 0)
            {
                // 用量已被并发任务归集，回收本张空账单，避免出现 0 金额账单
                crmContractBillMapper.deleteCrmContractBillById(bill.getId());
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }

            result.setBillCount(result.getBillCount() + 1);
            result.setUsageCount(result.getUsageCount() + usageCount);
            result.setTotalSellAmount(result.getTotalSellAmount().add(bill.getTotalSellAmount()));
            result.setTotalCostAmount(result.getTotalCostAmount().add(bill.getTotalCostAmount()));
            result.setTotalGrossProfit(result.getTotalGrossProfit().add(bill.getTotalGrossProfit()));
            result.getBills().add(crmContractBillMapper.selectCrmContractBillDetail(bill.getId()));
        }
        return result;
    }

    /**
     * 差异调账：生成一条反向冲销用量，计入下一账期账单
     *
     * @param diff 差异
     * @param vo 调账参数
     */
    private void createReverseUsage(CrmContractBillDiff diff, CrmBillDiffHandleVo vo)
    {
        CrmContractUsage origin = diff.getUsageId() == null ? null
                : crmContractUsageMapper.selectCrmContractUsageEntity(diff.getUsageId());
        if (origin == null || origin.getContractId() == null)
        {
            throw new ServiceException("该差异没有匹配到合同，无法生成冲销用量，请选择「豁免」或「驳回」");
        }
        BigDecimal sellAmount = vo.getAdjustSellAmount();
        BigDecimal costAmount = vo.getAdjustCostAmount();
        if (sellAmount == null && costAmount == null)
        {
            // 未填金额时默认按原用量全额冲销
            sellAmount = nullToZero(origin.getSellAmount()).negate();
            costAmount = nullToZero(origin.getCostAmount()).negate();
        }
        sellAmount = nullToZero(sellAmount);
        costAmount = nullToZero(costAmount);
        if (sellAmount.compareTo(BigDecimal.ZERO) == 0 && costAmount.compareTo(BigDecimal.ZERO) == 0)
        {
            throw new ServiceException("应收调整额与成本调整额不能同时为 0");
        }

        CrmContractUsage reverse = new CrmContractUsage();
        reverse.setCallNo("ADJ-" + (StringUtils.isEmpty(diff.getCallNo()) ? String.valueOf(diff.getId()) : diff.getCallNo()));
        reverse.setContractId(origin.getContractId());
        reverse.setContractProductId(origin.getContractProductId());
        reverse.setCustomerId(origin.getCustomerId());
        reverse.setSupplierId(origin.getSupplierId());
        reverse.setProductId(origin.getProductId());
        reverse.setProductCode(origin.getProductCode());
        long originTokens = origin.getTotalTokens() == null ? 0L : origin.getTotalTokens();
        reverse.setTotalTokens(-originTokens);
        // 消耗费用随冲销取负，保持与 token 的符号一致
        reverse.setTotalFees(nullToZero(origin.getTotalFees()).negate());
        reverse.setActualFees(nullToZero(origin.getActualFees()).negate());
        reverse.setBaseSellPrice(origin.getBaseSellPrice());
        reverse.setSellDiscount(origin.getSellDiscount());
        reverse.setBaseCostPrice(origin.getBaseCostPrice());
        reverse.setCostDiscount(origin.getCostDiscount());
        reverse.setSellAmount(sellAmount);
        reverse.setCostAmount(costAmount);
        reverse.setGrossProfit(sellAmount.subtract(costAmount));
        reverse.setMatchStatus(UsageMatchStatusEnum.SUCCESS.getCode());
        reverse.setMatchMsg("差异调账冲销，差异ID " + diff.getId());
        // 冲销计入当前账期，避免改动已确认的历史账单
        reverse.setCallTime(new Date());
        reverse.setUsageType(1);
        reverse.setRefUsageId(origin.getId());
        reverse.setCreateTime(new Date());
        crmContractUsageMapper.insertCrmContractUsage(reverse);
    }

    /**
     * 结算完成后回写商机结算状态
     *
     * @param contractId 合同ID
     */
    private void writeBackBusinessSettle(Long contractId)
    {
        if (contractId == null)
        {
            return;
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(contractId);
        if (contract == null || contract.getBusId() == null)
        {
            return;
        }
        crmBusinessMapper.updateSettleStatus(contract.getBusId(), 1, currentUserId());
    }

    /**
     * 当前登录用户ID，定时任务等无上下文场景回退到系统账号
     *
     * @return 用户ID
     */
    private Long currentUserId()
    {
        if (!crmFinanceDataScope.hasLoginContext())
        {
            return SYSTEM_USER_ID;
        }
        return SecurityUtils.getUserId();
    }

    /**
     * null 转 0
     *
     * @param value 金额
     * @return 金额
     */
    private BigDecimal nullToZero(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 格式化时间给查询参数使用
     *
     * @param date 时间
     * @return yyyy-MM-dd HH:mm:ss
     */
    private String formatDateTime(Date date)
    {
        return date == null ? null : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
