package com.ruoyi.crm.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.crm.mapper.SysNoGeneratorMapper;

/**
 * 账单编号生成器
 *
 * <p>编号规则：{@code BL-{渠道ID}-{yyyyMM}-{4位序列}}，示例 {@code BL-12-202611-0001}。
 * 序列按 biz_type + 渠道 + 账期维度在 sys_no_generator 内自增，依赖唯一索引保证并发不重复。</p>
 *
 * <p>使用 REQUIRES_NEW 独立事务：账单生成失败回滚时已分配序号不回滚，避免号码复用。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmBillNoGenerator
{
    /** 业务类型 */
    private static final String BIZ_TYPE = "bill";

    /** 账单编号前缀 */
    private static final String PREFIX = "BL";

    /** 序列位数 */
    private static final int SEQ_LENGTH = 4;

    @Autowired
    private SysNoGeneratorMapper sysNoGeneratorMapper;

    /**
     * 生成账单编号
     *
     * @param supplierId 渠道ID
     * @param billCycle 账期 yyyy-MM
     * @return 账单编号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generate(Long supplierId, String billCycle)
    {
        String supplier = supplierId == null ? "0" : String.valueOf(supplierId);
        String cycle = billCycle == null ? "" : billCycle.replace("-", "");
        sysNoGeneratorMapper.incrementSeq(BIZ_TYPE, supplier, cycle);
        Integer seq = sysNoGeneratorMapper.selectSeq(BIZ_TYPE, supplier, cycle);
        if (seq == null || seq <= 0)
        {
            throw new ServiceException("账单编号生成失败，请重试");
        }
        return PREFIX + "-" + supplier + "-" + cycle + "-" + String.format("%0" + SEQ_LENGTH + "d", seq);
    }
}
