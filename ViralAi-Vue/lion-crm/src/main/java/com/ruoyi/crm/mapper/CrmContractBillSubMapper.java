package com.ruoyi.crm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmContractBillSub;
import com.ruoyi.crm.dto.CrmContractBillSubDto;

/**
 * 子账单Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmContractBillSubMapper
{
    /**
     * 查询子账单列表
     *
     * @param query 查询条件
     * @return 子账单列表
     */
    public List<CrmContractBillSubDto> selectCrmContractBillSubList(CrmContractBillSubDto query);

    /**
     * 批量新增子账单（账单生成时按天写入）
     *
     * @param list 子账单集合
     * @return 影响条数
     */
    public int batchInsertCrmContractBillSub(@Param("list") List<CrmContractBillSub> list);

    /**
     * 按父账单统计子账单条数
     *
     * @param billId 父账单ID
     * @return 条数
     */
    public int countByBillId(@Param("billId") Long billId);

    /**
     * 删除父账单名下的全部子账单（重新出账时使用）
     *
     * @param billId 父账单ID
     * @return 影响条数
     */
    public int deleteByBillId(@Param("billId") Long billId);
}
