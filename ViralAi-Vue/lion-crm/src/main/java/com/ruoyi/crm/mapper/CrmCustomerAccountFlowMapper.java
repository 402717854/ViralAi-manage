package com.ruoyi.crm.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmCustomerAccountFlow;
import com.ruoyi.crm.dto.CrmCustomerAccountFlowDto;

/**
 * 客户资金流水Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmCustomerAccountFlowMapper
{
    /**
     * 新增流水
     *
     * @param flow 流水
     * @return 结果
     */
    public int insertFlow(CrmCustomerAccountFlow flow);

    /**
     * 流水分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerAccountFlowDto> selectFlowList(CrmCustomerAccountFlowDto query);

    /**
     * 按账户汇总流水（日终校验：入账 - 出账 应等于余额）
     *
     * @param accountId 账户ID
     * @return 汇总值
     */
    public BigDecimal sumSignedAmount(@Param("accountId") Long accountId);

    /**
     * 按业务类型 + 业务ID查询流水（结算撤销时反查原流水）
     *
     * @param bizType 业务类型
     * @param bizId 业务单据ID
     * @return 流水，不存在返回 null
     */
    public CrmCustomerAccountFlow selectByBiz(@Param("bizType") Integer bizType,
                                              @Param("bizId") Long bizId);

    /**
     * 查询账户最近一条流水
     *
     * @param accountId 账户ID
     * @return 流水
     */
    public CrmCustomerAccountFlow selectLastFlow(@Param("accountId") Long accountId);
}
