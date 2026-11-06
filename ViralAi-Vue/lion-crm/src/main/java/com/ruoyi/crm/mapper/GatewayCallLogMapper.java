package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.GatewayCallLogDto;

/**
 * 网关调用流水Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface GatewayCallLogMapper
{
    /**
     * 查询流水列表
     *
     * @param query 查询条件
     * @return 流水列表
     */
    public List<GatewayCallLogDto> selectGatewayCallLogList(GatewayCallLogDto query);

    /**
     * 查询流水详情
     *
     * @param id 主键
     * @return 流水
     */
    public GatewayCallLog selectGatewayCallLogById(Long id);

    /**
     * 入库流水（call_no 唯一索引兜底幂等）
     *
     * @param callLog 流水
     * @return 结果
     */
    public int insertGatewayCallLog(GatewayCallLog callLog);

    /**
     * 查询待对账流水：调用成功且尚未生成台账
     *
     * @param startTime 起始（按调用时间）
     * @param endTime 结束
     * @param supplierId 渠道ID，可为空
     * @return 流水列表
     */
    public List<GatewayCallLog> selectReconcileCallLogs(@Param("startTime") Date startTime,
                                                       @Param("endTime") Date endTime,
                                                       @Param("supplierId") Long supplierId);

    /**
     * 按流水号统计（幂等预检，可不查库直接依赖唯一索引）
     *
     * @param callNo 流水号
     * @return 条数
     */
    public int countByCallNo(@Param("callNo") String callNo);
}
