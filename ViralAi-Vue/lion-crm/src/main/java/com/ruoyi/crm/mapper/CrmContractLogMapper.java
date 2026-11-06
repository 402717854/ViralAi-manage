package com.ruoyi.crm.mapper;

import java.util.List;

import com.ruoyi.crm.domain.CrmContractLog;

/**
 * 合同操作日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractLogMapper
{
    /**
     * 新增操作日志
     *
     * @param log 日志
     * @return 结果
     */
    public int insertCrmContractLog(CrmContractLog log);

    /**
     * 查询合同全部操作日志（倒序）
     *
     * @param contractId 合同主键
     * @return 日志集合
     */
    public List<CrmContractLog> selectContractLogList(Long contractId);

    /**
     * 查询合同审批记录（仅审批通过与驳回）
     *
     * @param contractId 合同主键
     * @return 审批记录集合
     */
    public List<CrmContractLog> selectContractAuditList(Long contractId);
}
