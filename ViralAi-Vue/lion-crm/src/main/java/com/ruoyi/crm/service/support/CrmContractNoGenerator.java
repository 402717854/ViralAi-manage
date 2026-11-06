package com.ruoyi.crm.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.crm.mapper.SysNoGeneratorMapper;

/**
 * 合同编号生成器
 *
 * <p>编号规则：{@code HT-C{客户ID6位}-{yyyyMMdd}-{6位序列}}，示例 {@code HT-C000123-20261104-000001}。
 * 合同不再绑定单一渠道，编号以客户为主体。</p>
 *
 * <p>序列按 biz_type + 客户编号 + gen_date 维度在 sys_no_generator 表内自增，
 * 依赖唯一索引与行锁保证并发不重复；编号首次保存时生成，此后驳回、编辑均不再变更，
 * 草稿删除后序号废弃不回收。</p>
 *
 * <p>使用 REQUIRES_NEW 独立事务：合同保存失败回滚时，已分配的序号不回滚，避免号码复用。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmContractNoGenerator
{
    /** 业务类型 */
    private static final String BIZ_TYPE = "contract";

    /** 合同编号前缀 */
    private static final String PREFIX = "HT";

    /** 客户编号前缀：编号规则不再依赖渠道，改用客户主体 */
    private static final String CUSTOMER_PREFIX = "C";

    /** 客户ID补零位数 */
    private static final int CUSTOMER_ID_LENGTH = 6;

    /** 客户缺失时的兜底编号 */
    private static final String DEFAULT_CUSTOMER_CODE = "C000000";

    private static final int MAX_SEQ_LENGTH = 6;

    @Autowired
    private SysNoGeneratorMapper sysNoGeneratorMapper;

    /**
     * 生成合同编号
     * <p>
     * 规则：{@code HT-C{客户ID6位}-{yyyyMMdd}-{6位序列}}，示例 {@code HT-C000123-20261104-000001}。
     * 合同不再绑定单一渠道，编号以客户为主体；序列按客户 + 日期维度自增。
     *
     * @param customerId 客户ID，为空时使用兜底编号
     * @return 合同编号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String generate(Long customerId)
    {
        String code = customerId == null
                ? DEFAULT_CUSTOMER_CODE
                : CUSTOMER_PREFIX + String.format("%0" + CUSTOMER_ID_LENGTH + "d", customerId);
        String day = DateUtils.dateTimeNow("yyyyMMdd");
        sysNoGeneratorMapper.incrementSeq(BIZ_TYPE, code, day);
        Integer seq = sysNoGeneratorMapper.selectSeq(BIZ_TYPE, code, day);
        if (seq == null || seq <= 0)
        {
            throw new ServiceException("合同编号生成失败，请重试");
        }
        return PREFIX + "-" + code + "-" + day + "-" + String.format("%0" + MAX_SEQ_LENGTH + "d", seq);
    }
}
