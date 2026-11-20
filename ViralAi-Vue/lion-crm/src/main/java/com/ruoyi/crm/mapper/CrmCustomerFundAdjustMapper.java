package com.ruoyi.crm.mapper;

import com.ruoyi.crm.domain.CrmCustomerFundAdjust;

/**
 * 客户资金人工调账Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmCustomerFundAdjustMapper
{
    /**
     * 新增调账单
     *
     * @param adjust 调账单
     * @return 结果
     */
    public int insertAdjust(CrmCustomerFundAdjust adjust);

    /**
     * 按ID查询调账单
     *
     * @param id 主键
     * @return 调账单
     */
    public CrmCustomerFundAdjust selectById(Long id);
}
