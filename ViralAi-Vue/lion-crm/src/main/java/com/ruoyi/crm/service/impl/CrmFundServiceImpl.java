package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.domain.CrmCustomerAccount;
import com.ruoyi.crm.domain.CrmCustomerAccountFlow;
import com.ruoyi.crm.domain.CrmCustomerFundAdjust;
import com.ruoyi.crm.domain.CrmCustomerRecharge;
import com.ruoyi.crm.domain.CrmCustomerRefund;
import com.ruoyi.crm.dto.CrmCustomerAccountDto;
import com.ruoyi.crm.dto.CrmCustomerAccountFlowDto;
import com.ruoyi.crm.dto.CrmCustomerRechargeDto;
import com.ruoyi.crm.dto.CrmCustomerRefundDto;
import com.ruoyi.crm.dto.CrmFundAdjustVo;
import com.ruoyi.crm.dto.CrmFundAuditVo;
import com.ruoyi.crm.dto.CrmFundChangeCmd;
import com.ruoyi.crm.dto.CrmFundCheckVo;
import com.ruoyi.crm.dto.CrmFundInitVo;
import com.ruoyi.crm.dto.CrmFundRechargeVo;
import com.ruoyi.crm.dto.CrmFundRefundVo;
import com.ruoyi.crm.dto.CrmFundSummaryVo;
import com.ruoyi.crm.enums.CustomerFundBizTypeEnum;
import com.ruoyi.crm.mapper.CrmCustomerAccountFlowMapper;
import com.ruoyi.crm.mapper.CrmCustomerAccountMapper;
import com.ruoyi.crm.mapper.CrmCustomerFundAdjustMapper;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
import com.ruoyi.crm.mapper.CrmCustomerRechargeMapper;
import com.ruoyi.crm.mapper.CrmCustomerRefundMapper;
import com.ruoyi.crm.service.ICrmFundService;
import com.ruoyi.crm.service.support.CrmCustomerFundLedger;
import com.ruoyi.crm.service.support.CrmFundDataScope;
import com.ruoyi.crm.service.support.CrmFundNoGenerator;

/**
 * 客户资金Service实现
 *
 * <p>资金类写操作一律经过 {@link CrmCustomerFundLedger}，本类只负责业务校验与单据状态流转。
 * 单据状态更新都带「期望状态」条件，并发重复审核时第二次影响行数为 0，据此报错。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Service
public class CrmFundServiceImpl implements ICrmFundService
{
    /** 充值单：待审核 */
    private static final int RECHARGE_PENDING = 0;

    /** 充值单：已入账 */
    private static final int RECHARGE_POSTED = 1;

    /** 充值单 / 退款单：已驳回 */
    private static final int STATUS_REJECTED = 2;

    /** 退款单：待审批 */
    private static final int REFUND_PENDING = 0;

    /** 退款单：已退款 */
    private static final int REFUND_DONE = 1;

    /** 调账方向：调增 */
    private static final int ADJUST_ADD = 1;

    /** 调账方向：调减 */
    private static final int ADJUST_SUB = 2;

    @Autowired
    private CrmCustomerAccountMapper crmCustomerAccountMapper;

    @Autowired
    private CrmCustomerAccountFlowMapper crmCustomerAccountFlowMapper;

    @Autowired
    private CrmCustomerRechargeMapper crmCustomerRechargeMapper;

    @Autowired
    private CrmCustomerRefundMapper crmCustomerRefundMapper;

    @Autowired
    private CrmCustomerFundAdjustMapper crmCustomerFundAdjustMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private CrmCustomerFundLedger crmCustomerFundLedger;

    @Autowired
    private CrmFundDataScope crmFundDataScope;

    @Autowired
    private CrmFundNoGenerator crmFundNoGenerator;

    @Override
    public List<CrmCustomerAccountDto> selectAccountList(CrmCustomerAccountDto query)
    {
        crmFundDataScope.applyScope(query);
        List<CrmCustomerAccountDto> list = crmCustomerAccountMapper.selectAccountList(query);
        for (CrmCustomerAccountDto item : list)
        {
            BigDecimal balance = item.getBalance() == null ? BigDecimal.ZERO : item.getBalance();
            BigDecimal frozen = item.getFrozenAmount() == null ? BigDecimal.ZERO : item.getFrozenAmount();
            item.setAvailableBalance(balance.subtract(frozen));
        }
        return list;
    }

    @Override
    public CrmCustomerAccountDto selectAccountDetail(Long customerId)
    {
        if (customerId == null)
        {
            throw new ServiceException("客户ID不能为空");
        }
        crmFundDataScope.assertCustomerAccessible(customerId);
        CrmCustomerAccountDto detail = crmCustomerAccountMapper.selectAccountDetail(customerId);
        if (detail == null)
        {
            throw new ServiceException("客户不存在");
        }
        BigDecimal balance = detail.getBalance() == null ? BigDecimal.ZERO : detail.getBalance();
        BigDecimal frozen = detail.getFrozenAmount() == null ? BigDecimal.ZERO : detail.getFrozenAmount();
        detail.setAvailableBalance(balance.subtract(frozen));
        return detail;
    }

    @Override
    public CrmFundSummaryVo selectSummary()
    {
        String owner = crmFundDataScope.scopeOwnerUser();
        CrmFundSummaryVo summary = crmCustomerAccountMapper.selectFundSummary(monthStart(), owner);
        if (summary == null)
        {
            summary = new CrmFundSummaryVo();
        }
        if (summary.getTotalBalance() == null)
        {
            summary.setTotalBalance(BigDecimal.ZERO);
        }
        summary.setPendingRechargeCount(crmCustomerRechargeMapper.countPending(owner));
        return summary;
    }

    @Override
    public List<CrmCustomerAccountFlowDto> selectFlowList(CrmCustomerAccountFlowDto query)
    {
        crmFundDataScope.applyScope(query);
        List<CrmCustomerAccountFlowDto> list = crmCustomerAccountFlowMapper.selectFlowList(query);
        for (CrmCustomerAccountFlowDto item : list)
        {
            CustomerFundBizTypeEnum type = CustomerFundBizTypeEnum.getByCode(item.getBizType());
            item.setBizTypeName(type == null ? "-" : type.getName());
        }
        return list;
    }

    @Override
    public List<CrmCustomerRechargeDto> selectRechargeList(CrmCustomerRechargeDto query)
    {
        crmFundDataScope.applyScope(query);
        List<CrmCustomerRechargeDto> list = crmCustomerRechargeMapper.selectRechargeList(query);
        for (CrmCustomerRechargeDto item : list)
        {
            item.setPayTypeName(payTypeName(item.getPayType()));
            item.setStatusName(rechargeStatusName(item.getStatus()));
        }
        return list;
    }

    @Override
    public List<CrmCustomerRefundDto> selectRefundList(CrmCustomerRefundDto query)
    {
        crmFundDataScope.applyScope(query);
        List<CrmCustomerRefundDto> list = crmCustomerRefundMapper.selectRefundList(query);
        for (CrmCustomerRefundDto item : list)
        {
            item.setRefundTypeName(item.getRefundType() != null && item.getRefundType() == 1 ? "原路退回" : "其他方式");
            item.setStatusName(refundStatusName(item.getStatus()));
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createRecharge(CrmFundRechargeVo vo)
    {
        if (vo == null || vo.getCustomerId() == null)
        {
            throw new ServiceException("请选择客户");
        }
        if (vo.getAmount() == null || vo.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("充值金额必须大于 0");
        }
        crmFundDataScope.assertCustomerAccessible(vo.getCustomerId());
        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(vo.getCustomerId());
        if (customer == null)
        {
            throw new ServiceException("客户不存在");
        }

        CrmCustomerRecharge recharge = new CrmCustomerRecharge();
        recharge.setRechargeNo(crmFundNoGenerator.generateRechargeNo());
        recharge.setCustomerId(vo.getCustomerId());
        recharge.setAmount(vo.getAmount());
        recharge.setPayType(vo.getPayType() == null ? 1 : vo.getPayType());
        recharge.setPayerName(StringUtils.isNotEmpty(vo.getPayerName()) ? vo.getPayerName() : customer.getName());
        recharge.setPayerAccount(vo.getPayerAccount());
        recharge.setPayTime(vo.getPayTime() == null ? new Date() : vo.getPayTime());
        recharge.setBankSerialNo(StringUtils.isNotEmpty(vo.getBankSerialNo()) ? vo.getBankSerialNo().trim() : null);
        recharge.setVoucherUrl(vo.getVoucherUrl());
        recharge.setRemark(vo.getRemark());
        recharge.setStatus(RECHARGE_PENDING);
        recharge.setCreator(currentUserId());
        try
        {
            crmCustomerRechargeMapper.insertRecharge(recharge);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("该银行流水号已登记过充值单，请勿重复登记");
        }
        return recharge.getRechargeNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRecharge(CrmFundAuditVo vo)
    {
        if (vo == null || vo.getId() == null)
        {
            throw new ServiceException("充值单ID不能为空");
        }
        if (vo.getPass() == null)
        {
            throw new ServiceException("请选择审核结果");
        }
        CrmCustomerRecharge recharge = crmCustomerRechargeMapper.selectByIdForUpdate(vo.getId());
        if (recharge == null)
        {
            throw new ServiceException("充值单不存在");
        }
        crmFundDataScope.assertCustomerAccessible(recharge.getCustomerId());
        if (recharge.getStatus() == null || recharge.getStatus() != RECHARGE_PENDING)
        {
            throw new ServiceException("该充值单已审核，请勿重复操作");
        }
        String remark = StringUtils.isNotEmpty(vo.getRemark()) ? vo.getRemark()
                : (vo.getPass() ? "审核通过" : "审核驳回");

        if (!vo.getPass())
        {
            int rows = crmCustomerRechargeMapper.updateStatus(recharge.getId(), RECHARGE_PENDING,
                    STATUS_REJECTED, currentUserId(), new Date(), remark);
            if (rows == 0)
            {
                throw new ServiceException("充值单状态已变更，请刷新后重试");
            }
            return;
        }

        CrmFundChangeCmd cmd = CrmFundChangeCmd.of(recharge.getCustomerId(),
                CustomerFundBizTypeEnum.RECHARGE.getCode(), recharge.getId(), recharge.getAmount());
        cmd.setBizNo(recharge.getRechargeNo());
        cmd.setRemark("充值入账 " + recharge.getRechargeNo());
        crmCustomerFundLedger.change(cmd);

        int rows = crmCustomerRechargeMapper.updateStatus(recharge.getId(), RECHARGE_PENDING,
                RECHARGE_POSTED, currentUserId(), new Date(), remark);
        if (rows == 0)
        {
            throw new ServiceException("充值单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createRefund(CrmFundRefundVo vo)
    {
        if (vo == null || vo.getCustomerId() == null)
        {
            throw new ServiceException("请选择客户");
        }
        if (vo.getAmount() == null || vo.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("退款金额必须大于 0");
        }
        if (StringUtils.isEmpty(vo.getReason()))
        {
            throw new ServiceException("请填写退款原因");
        }
        crmFundDataScope.assertCustomerAccessible(vo.getCustomerId());
        BigDecimal available = crmCustomerFundLedger.availableBalance(vo.getCustomerId());
        if (vo.getAmount().compareTo(available) > 0)
        {
            throw new ServiceException("退款金额不能超过可用余额 " + available.toPlainString());
        }

        CrmCustomerRefund refund = new CrmCustomerRefund();
        refund.setRefundNo(crmFundNoGenerator.generateRefundNo());
        refund.setCustomerId(vo.getCustomerId());
        refund.setAmount(vo.getAmount());
        refund.setRefundType(vo.getRefundType() == null ? 1 : vo.getRefundType());
        refund.setReceiveName(vo.getReceiveName());
        refund.setReceiveAccount(vo.getReceiveAccount());
        refund.setReceiveBank(vo.getReceiveBank());
        refund.setReason(vo.getReason());
        refund.setStatus(REFUND_PENDING);
        refund.setCreator(currentUserId());
        crmCustomerRefundMapper.insertRefund(refund);
        return refund.getRefundNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(CrmFundAuditVo vo)
    {
        if (vo == null || vo.getId() == null)
        {
            throw new ServiceException("退款单ID不能为空");
        }
        if (vo.getPass() == null)
        {
            throw new ServiceException("请选择审批结果");
        }
        CrmCustomerRefund refund = crmCustomerRefundMapper.selectByIdForUpdate(vo.getId());
        if (refund == null)
        {
            throw new ServiceException("退款单不存在");
        }
        crmFundDataScope.assertCustomerAccessible(refund.getCustomerId());
        if (refund.getStatus() == null || refund.getStatus() != REFUND_PENDING)
        {
            throw new ServiceException("该退款单已审批，请勿重复操作");
        }
        String remark = StringUtils.isNotEmpty(vo.getRemark()) ? vo.getRemark()
                : (vo.getPass() ? "审批通过" : "审批驳回");

        if (!vo.getPass())
        {
            int rows = crmCustomerRefundMapper.updateStatus(refund.getId(), REFUND_PENDING,
                    STATUS_REJECTED, currentUserId(), new Date(), remark);
            if (rows == 0)
            {
                throw new ServiceException("退款单状态已变更，请刷新后重试");
            }
            return;
        }

        // 审批通过才真正扣款：申请时校验过一次，但期间可能已被结算占用，这里必须再校验
        CrmFundChangeCmd cmd = CrmFundChangeCmd.of(refund.getCustomerId(),
                CustomerFundBizTypeEnum.REFUND.getCode(), refund.getId(), refund.getAmount());
        cmd.setBizNo(refund.getRefundNo());
        cmd.setRemark("退款 " + refund.getRefundNo() + "：" + refund.getReason());
        crmCustomerFundLedger.change(cmd);

        int rows = crmCustomerRefundMapper.updateStatus(refund.getId(), REFUND_PENDING,
                REFUND_DONE, currentUserId(), new Date(), remark);
        if (rows == 0)
        {
            throw new ServiceException("退款单状态已变更，请刷新后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmCustomerAccountFlow adjust(CrmFundAdjustVo vo)
    {
        if (vo == null || vo.getCustomerId() == null)
        {
            throw new ServiceException("请选择客户");
        }
        if (vo.getAmount() == null || vo.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("调账金额必须大于 0");
        }
        if (vo.getDirection() == null || (vo.getDirection() != ADJUST_ADD && vo.getDirection() != ADJUST_SUB))
        {
            throw new ServiceException("调账方向只能为调增或调减");
        }
        if (StringUtils.isEmpty(vo.getReason()))
        {
            throw new ServiceException("请填写调账原因");
        }
        crmFundDataScope.assertCustomerAccessible(vo.getCustomerId());

        CrmCustomerFundAdjust adjust = new CrmCustomerFundAdjust();
        adjust.setAdjustNo(crmFundNoGenerator.generateAdjustNo());
        adjust.setCustomerId(vo.getCustomerId());
        adjust.setAmount(vo.getAmount());
        adjust.setDirection(vo.getDirection());
        adjust.setReason(vo.getReason());
        adjust.setCreator(currentUserId());
        crmCustomerFundAdjustMapper.insertAdjust(adjust);

        CustomerFundBizTypeEnum bizType = vo.getDirection() == ADJUST_ADD
                ? CustomerFundBizTypeEnum.ADJUST_ADD : CustomerFundBizTypeEnum.ADJUST_SUB;
        CrmFundChangeCmd cmd = CrmFundChangeCmd.of(vo.getCustomerId(), bizType.getCode(),
                adjust.getId(), vo.getAmount());
        cmd.setBizNo(adjust.getAdjustNo());
        cmd.setRemark(vo.getReason());
        return crmCustomerFundLedger.change(cmd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importInitBalance(CrmFundInitVo vo)
    {
        if (vo == null || vo.getItems() == null || vo.getItems().isEmpty())
        {
            throw new ServiceException("导入明细不能为空");
        }
        int success = 0;
        for (CrmFundInitVo.Item item : vo.getItems())
        {
            if (item == null || item.getCustomerId() == null)
            {
                throw new ServiceException("导入明细存在未选择客户的行");
            }
            BigDecimal amount = item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
            if (amount.compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("期初余额不能为负数");
            }
            CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(item.getCustomerId());
            if (customer == null)
            {
                throw new ServiceException("客户不存在：" + item.getCustomerId());
            }
            if (amount.compareTo(BigDecimal.ZERO) == 0)
            {
                // 0 期初不生成流水，避免制造无意义记录
                continue;
            }
            CrmFundChangeCmd cmd = CrmFundChangeCmd.of(item.getCustomerId(),
                    CustomerFundBizTypeEnum.INIT.getCode(), item.getCustomerId(), amount);
            cmd.setBizNo("期初余额");
            cmd.setRemark(StringUtils.isNotEmpty(item.getRemark()) ? item.getRemark() : "上线期初余额导入");
            try
            {
                crmCustomerFundLedger.change(cmd);
            }
            catch (ServiceException e)
            {
                throw new ServiceException("客户[" + customer.getName() + "]期初导入失败：" + e.getMessage());
            }
            success++;
        }
        return success;
    }

    @Override
    public List<CrmFundCheckVo> checkConsistency()
    {
        List<CrmCustomerAccount> accounts = crmCustomerAccountMapper.selectAllAccounts();
        List<CrmFundCheckVo> mismatches = new ArrayList<>();
        for (CrmCustomerAccount account : accounts)
        {
            BigDecimal balance = account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
            BigDecimal flowSum = crmCustomerAccountFlowMapper.sumSignedAmount(account.getId());
            if (flowSum == null)
            {
                flowSum = BigDecimal.ZERO;
            }
            if (balance.compareTo(flowSum) == 0)
            {
                continue;
            }
            CrmFundCheckVo item = new CrmFundCheckVo();
            item.setAccountId(account.getId());
            item.setCustomerId(account.getCustomerId());
            CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(account.getCustomerId());
            item.setCustomerName(customer == null ? "-" : customer.getName());
            item.setBalance(balance);
            item.setFlowSum(flowSum);
            item.setDiff(balance.subtract(flowSum));
            mismatches.add(item);
        }
        return mismatches;
    }

    @Override
    public int countPendingRecharge()
    {
        return crmCustomerRechargeMapper.countPending(crmFundDataScope.scopeOwnerUser());
    }

    /**
     * 本月起始时间（汇总卡片口径：本月充值/扣减/退款）
     *
     * @return 本月 1 日 00:00:00
     */
    private Date monthStart()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    /**
     * 当前登录用户ID
     *
     * @return 用户ID
     */
    private Long currentUserId()
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            return userId == null ? 1L : userId;
        }
        catch (Exception e)
        {
            return 1L;
        }
    }

    /**
     * 付款方式名称
     *
     * @param payType 编码
     * @return 名称
     */
    private String payTypeName(Integer payType)
    {
        if (payType == null)
        {
            return "-";
        }
        switch (payType)
        {
            case 1:
                return "对公转账";
            case 2:
                return "在线支付";
            case 3:
                return "现金";
            case 4:
                return "其他";
            default:
                return "-";
        }
    }

    /**
     * 充值单状态名称
     *
     * @param status 编码
     * @return 名称
     */
    private String rechargeStatusName(Integer status)
    {
        if (status == null)
        {
            return "-";
        }
        switch (status)
        {
            case RECHARGE_PENDING:
                return "待审核";
            case RECHARGE_POSTED:
                return "已入账";
            case STATUS_REJECTED:
                return "已驳回";
            default:
                return "-";
        }
    }

    /**
     * 退款单状态名称
     *
     * @param status 编码
     * @return 名称
     */
    private String refundStatusName(Integer status)
    {
        if (status == null)
        {
            return "-";
        }
        switch (status)
        {
            case REFUND_PENDING:
                return "待审批";
            case REFUND_DONE:
                return "已退款";
            case STATUS_REJECTED:
                return "已驳回";
            default:
                return "-";
        }
    }
}
