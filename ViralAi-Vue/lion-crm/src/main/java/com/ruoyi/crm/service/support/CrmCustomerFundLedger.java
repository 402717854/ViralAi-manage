package com.ruoyi.crm.service.support;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmCustomerAccount;
import com.ruoyi.crm.domain.CrmCustomerAccountFlow;
import com.ruoyi.crm.dto.CrmFundChangeCmd;
import com.ruoyi.crm.enums.CustomerFundBizTypeEnum;
import com.ruoyi.crm.mapper.CrmCustomerAccountFlowMapper;
import com.ruoyi.crm.mapper.CrmCustomerAccountMapper;

/**
 * 客户资金账本
 *
 * <p><b>所有</b>账户余额变动都必须走 {@link #change(CrmFundChangeCmd)}，
 * 由本类统一完成「锁账户 → 校验余额 → 写流水 → 更新余额」。
 * 禁止任何业务代码直接 update 余额：绕过账本必然导致流水与余额不一致，
 * 而账实一致是这套系统唯一不能妥协的指标。</p>
 *
 * <p>并发控制采用悲观锁（账户行 for update）：结算频率低（每月每客户几张账单），
 * 冲突极少，行锁比乐观锁重试逻辑更简单可靠；version 作为漏加锁写路径的兜底。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Component
public class CrmCustomerFundLedger
{
    /** 无登录上下文（定时任务）时的操作人 */
    private static final Long SYSTEM_USER_ID = 1L;

    @Autowired
    private CrmCustomerAccountMapper crmCustomerAccountMapper;

    @Autowired
    private CrmCustomerAccountFlowMapper crmCustomerAccountFlowMapper;

    @Autowired
    private CrmFundNoGenerator crmFundNoGenerator;

    /**
     * 查询可用余额（账户不存在视为 0）
     *
     * @param customerId 客户ID
     * @return 可用余额
     */
    public BigDecimal availableBalance(Long customerId)
    {
        if (customerId == null)
        {
            return BigDecimal.ZERO;
        }
        CrmCustomerAccount account = crmCustomerAccountMapper.selectByCustomerId(customerId);
        return account == null ? BigDecimal.ZERO : account.available();
    }

    /**
     * 校验余额是否充足
     *
     * @param customerId 客户ID
     * @param amount 需要的金额
     */
    public void assertSufficient(Long customerId, BigDecimal amount)
    {
        BigDecimal available = availableBalance(customerId);
        if (available.compareTo(amount) < 0)
        {
            throw new ServiceException(String.format("客户可用余额不足：可用 %s，本次需扣减 %s",
                    available.toPlainString(), amount.toPlainString()));
        }
    }

    /**
     * 确保账户存在（首次充值时自动开户）
     *
     * @param customerId 客户ID
     * @return 账户
     */
    public CrmCustomerAccount ensureAccount(Long customerId)
    {
        CrmCustomerAccount account = crmCustomerAccountMapper.selectByCustomerId(customerId);
        if (account != null)
        {
            return account;
        }
        CrmCustomerAccount created = new CrmCustomerAccount();
        created.setCustomerId(customerId);
        created.setBalance(BigDecimal.ZERO);
        created.setFrozenAmount(BigDecimal.ZERO);
        created.setTotalRecharge(BigDecimal.ZERO);
        created.setTotalDeduct(BigDecimal.ZERO);
        created.setTotalRefund(BigDecimal.ZERO);
        created.setStatus(1);
        created.setLastFlowTime(new Date());
        try
        {
            crmCustomerAccountMapper.insertAccount(created);
            return created;
        }
        catch (DuplicateKeyException e)
        {
            // 并发开户：唯一键 uk_customer_id 兜底，回查即可
            return crmCustomerAccountMapper.selectByCustomerId(customerId);
        }
    }

    /**
     * 锁定账户（不存在则先开户再锁）
     *
     * @param customerId 客户ID
     * @return 已加行锁的账户
     */
    private CrmCustomerAccount lockAccount(Long customerId)
    {
        CrmCustomerAccount account = crmCustomerAccountMapper.selectByCustomerIdForUpdate(customerId);
        if (account == null)
        {
            ensureAccount(customerId);
            account = crmCustomerAccountMapper.selectByCustomerIdForUpdate(customerId);
        }
        if (account == null)
        {
            throw new ServiceException("客户资金账户初始化失败，请重试");
        }
        return account;
    }

    /**
     * 执行一次资金变动
     *
     * @param cmd 变动指令
     * @return 生成的流水
     */
    @Transactional(rollbackFor = Exception.class)
    public CrmCustomerAccountFlow change(CrmFundChangeCmd cmd)
    {
        if (cmd == null || cmd.getCustomerId() == null)
        {
            throw new ServiceException("客户ID不能为空");
        }
        CustomerFundBizTypeEnum bizType = CustomerFundBizTypeEnum.getByCode(cmd.getBizType());
        if (bizType == null)
        {
            throw new ServiceException("资金变动类型不合法");
        }
        BigDecimal amount = cmd.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("资金变动金额必须大于 0");
        }
        if (cmd.getBizId() == null)
        {
            throw new ServiceException("业务单据ID不能为空");
        }

        CrmCustomerAccount account = lockAccount(cmd.getCustomerId());
        if (account.getStatus() != null && account.getStatus() == 0)
        {
            throw new ServiceException("客户资金账户已冻结，不能发生资金变动");
        }

        // 幂等预检：数据库唯一键 uk_biz 是最终保证，这里只是为了给出可读提示
        CrmCustomerAccountFlow exists = crmCustomerAccountFlowMapper
                .selectByBiz(bizType.getCode(), cmd.getBizId());
        if (exists != null)
        {
            throw new ServiceException("该单据已产生资金流水（" + exists.getFlowNo() + "），请勿重复操作");
        }

        BigDecimal before = nvl(account.getBalance());
        BigDecimal after = bizType.isIncome() ? before.add(amount) : before.subtract(amount);
        if (!bizType.isIncome() && after.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException(String.format("客户可用余额不足：当前余额 %s，本次需扣减 %s",
                    before.toPlainString(), amount.toPlainString()));
        }

        CrmCustomerAccountFlow flow = new CrmCustomerAccountFlow();
        flow.setFlowNo(crmFundNoGenerator.generateFlowNo());
        flow.setAccountId(account.getId());
        flow.setCustomerId(cmd.getCustomerId());
        flow.setBizType(bizType.getCode());
        flow.setBizId(cmd.getBizId());
        flow.setBizNo(cmd.getBizNo());
        flow.setDirection(bizType.getDirection());
        flow.setAmount(amount);
        flow.setBalanceBefore(before);
        flow.setBalanceAfter(after);
        flow.setBillId(cmd.getBillId());
        flow.setBillNo(cmd.getBillNo());
        flow.setContractId(cmd.getContractId());
        flow.setRemark(cmd.getRemark());
        flow.setOperator(cmd.getOperator() == null ? currentUserId() : cmd.getOperator());
        try
        {
            crmCustomerAccountFlowMapper.insertFlow(flow);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("该单据已产生资金流水，请勿重复操作");
        }

        BigDecimal totalRecharge = nvl(account.getTotalRecharge());
        BigDecimal totalDeduct = nvl(account.getTotalDeduct());
        BigDecimal totalRefund = nvl(account.getTotalRefund());
        switch (bizType)
        {
            case RECHARGE:
                totalRecharge = totalRecharge.add(amount);
                break;
            case SETTLE_DEDUCT:
                totalDeduct = totalDeduct.add(amount);
                break;
            case REFUND:
                totalRefund = totalRefund.add(amount);
                break;
            case REVERSE:
            case SETTLE_CANCEL:
                // 负数账单退回 / 结算撤销：冲减累计扣减，不重复计入充值
                totalDeduct = totalDeduct.subtract(amount);
                break;
            case SETTLE_CANCEL_BACK:
                // 撤销「负数账单退回」：把退回的钱重新计入扣减
                totalDeduct = totalDeduct.add(amount);
                break;
            default:
                // 人工调账、期初余额不进入三个累计口径
                break;
        }

        int rows = crmCustomerAccountMapper.updateAccountBalance(account.getId(), after,
                totalRecharge, totalDeduct, totalRefund, new Date(), account.getVersion());
        if (rows == 0)
        {
            throw new ServiceException("账户余额更新冲突，请重试");
        }
        return flow;
    }

    /**
     * 空值归零
     *
     * @param value 金额
     * @return 非空金额
     */
    private BigDecimal nvl(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 当前操作人，无登录上下文回退系统账号
     *
     * @return 用户ID
     */
    private Long currentUserId()
    {
        try
        {
            Long userId = SecurityUtils.getUserId();
            return userId == null ? SYSTEM_USER_ID : userId;
        }
        catch (Exception e)
        {
            return SYSTEM_USER_ID;
        }
    }
}
