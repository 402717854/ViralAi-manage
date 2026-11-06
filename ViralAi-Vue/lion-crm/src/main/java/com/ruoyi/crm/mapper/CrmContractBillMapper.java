package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmContractBill;
import com.ruoyi.crm.dto.CrmContractBillDto;

/**
 * 对账账单Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractBillMapper
{
    /**
     * 查询账单列表
     *
     * @param query 查询条件
     * @return 账单列表
     */
    public List<CrmContractBillDto> selectCrmContractBillList(CrmContractBillDto query);

    /**
     * 查询账单详情（含客户、渠道、合同名称）
     *
     * @param id 账单ID
     * @return 账单
     */
    public CrmContractBillDto selectCrmContractBillDetail(Long id);

    /**
     * 查询账单实体
     *
     * @param id 账单ID
     * @return 账单
     */
    public CrmContractBill selectCrmContractBillById(Long id);

    /**
     * 新增账单
     *
     * @param bill 账单
     * @return 结果
     */
    public int insertCrmContractBill(CrmContractBill bill);

    /**
     * 更新账单复核状态
     *
     * @param id 账单ID
     * @param billStatus 目标状态
     * @param remark 复核意见
     * @param auditor 复核人
     * @param auditTime 复核时间
     * @return 结果
     */
    public int updateBillAudit(@Param("id") Long id,
                               @Param("billStatus") Integer billStatus,
                               @Param("remark") String remark,
                               @Param("auditor") Long auditor,
                               @Param("auditTime") Date auditTime);

    /**
     * 统计某合同某渠道某账期已生成的账单数（用于标记补充账单）
     *
     * @param contractId 合同ID
     * @param supplierId 渠道ID
     * @param billCycle 账期
     * @return 条数
     */
    public int countByContractAndCycle(@Param("contractId") Long contractId,
                                       @Param("supplierId") Long supplierId,
                                       @Param("billCycle") String billCycle);

    /**
     * 统计某渠道某账期已生成的账单数
     *
     * @param supplierId 渠道ID
     * @param billCycle 账期
     * @return 条数
     */
    public int countBySupplierAndCycle(@Param("supplierId") Long supplierId,
                                       @Param("billCycle") String billCycle);

    /**
     * 统计某账期的账单数（看板概览）
     *
     * @param billCycle 账期
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @return 条数
     */
    public int countBillsByCycle(@Param("billCycle") String billCycle,
                                 @Param("supplierIds") List<Long> supplierIds);

    /**
     * 删除账单（仅用于生成过程中归集不到用量时回收空账单）
     *
     * @param id 账单ID
     * @return 结果
     */
    public int deleteCrmContractBillById(Long id);
}
