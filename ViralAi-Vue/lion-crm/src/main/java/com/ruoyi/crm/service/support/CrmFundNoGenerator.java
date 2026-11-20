package com.ruoyi.crm.service.support;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.crm.mapper.SysNoGeneratorMapper;

/**
 * 客户资金单据编号生成器
 *
 * <p>三类编号：充值单 {@code RC-yyyyMMdd-000001}、退款单 {@code RF-yyyyMMdd-000001}、
 * 资金流水 {@code FL-yyyyMMdd-000001}。序列在 sys_no_generator 内按
 * biz_type + gen_date 自增，唯一索引兜底并发。</p>
 *
 * <p>用 REQUIRES_NEW 独立事务：业务回滚时已分配序号不回滚，避免号码复用。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Service
public class CrmFundNoGenerator
{
    /** 充值单业务类型 */
    private static final String BIZ_RECHARGE = "fund_recharge";

    /** 退款单业务类型 */
    private static final String BIZ_REFUND = "fund_refund";

    /** 流水业务类型 */
    private static final String BIZ_FLOW = "fund_flow";

    /** 调账单业务类型 */
    private static final String BIZ_ADJUST = "fund_adjust";

    /** 序列位数 */
    private static final int SEQ_LENGTH = 6;

    /** 序列作用域固定值（资金单号不按渠道/客户分段，便于全局检索） */
    private static final String SCOPE = "0";

    @Autowired
    private SysNoGeneratorMapper sysNoGeneratorMapper;

    /**
     * 生成充值单号
     *
     * <p>@Transactional 必须落在 public 方法上：本类内部调用（this.next）不经过代理，
     * 写在私有方法上会静默退化为普通调用，独立事务失效。</p>
     *
     * @return RC-yyyyMMdd-000001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateRechargeNo()
    {
        return next(BIZ_RECHARGE, "RC");
    }

    /**
     * 生成退款单号
     *
     * @return RF-yyyyMMdd-000001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateRefundNo()
    {
        return next(BIZ_REFUND, "RF");
    }

    /**
     * 生成资金流水号
     *
     * @return FL-yyyyMMdd-000001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateFlowNo()
    {
        return next(BIZ_FLOW, "FL");
    }

    /**
     * 生成调账单号
     *
     * @return AD-yyyyMMdd-000001
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generateAdjustNo()
    {
        return next(BIZ_ADJUST, "AD");
    }

    /**
     * 分配序号并拼装编号
     *
     * @param bizType 业务类型
     * @param prefix 编号前缀
     * @return 编号
     */
    private String next(String bizType, String prefix)
    {
        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        sysNoGeneratorMapper.incrementSeq(bizType, SCOPE, day);
        Integer seq = sysNoGeneratorMapper.selectSeq(bizType, SCOPE, day);
        if (seq == null || seq <= 0)
        {
            throw new ServiceException("单据编号生成失败，请重试");
        }
        return prefix + "-" + day + "-" + String.format("%0" + SEQ_LENGTH + "d", seq);
    }
}
