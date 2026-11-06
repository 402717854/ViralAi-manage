package com.ruoyi.crm.mapper;

import java.util.List;

import com.ruoyi.crm.domain.CrmBusinessLog;

/**
 * 商机操作日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmBusinessLogMapper
{
    /**
     * 查询商机操作日志（按时间倒序）
     *
     * @param busId 商机主键
     * @return 日志集合
     */
    public List<CrmBusinessLog> selectCrmBusinessLogList(Long busId);

    /**
     * 新增操作日志
     *
     * @param log 日志
     * @return 结果
     */
    public int insertCrmBusinessLog(CrmBusinessLog log);
}
