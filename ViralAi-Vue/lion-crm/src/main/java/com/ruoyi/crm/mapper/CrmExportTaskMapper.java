package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmExportTask;

/**
 * 异步导出任务Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-15
 */
public interface CrmExportTaskMapper
{
    /**
     * 新增任务
     *
     * @param task 任务
     * @return 结果
     */
    public int insertExportTask(CrmExportTask task);

    /**
     * 查询任务
     *
     * @param id 主键
     * @return 任务
     */
    public CrmExportTask selectExportTaskById(Long id);

    /**
     * 标记为处理中
     *
     * @param id 主键
     * @param startTime 开始时间
     * @return 结果
     */
    public int markRunning(@Param("id") Long id, @Param("startTime") Date startTime);

    /**
     * 标记成功
     *
     * @param id 主键
     * @param filePath 落盘绝对路径
     * @param rowCount 导出行数
     * @param finishTime 完成时间
     * @return 结果
     */
    public int markSuccess(@Param("id") Long id, @Param("filePath") String filePath,
                           @Param("rowCount") Integer rowCount, @Param("finishTime") Date finishTime);

    /**
     * 标记失败
     *
     * @param id 主键
     * @param errMsg 失败原因（已截断）
     * @param finishTime 完成时间
     * @return 结果
     */
    public int markFailed(@Param("id") Long id, @Param("errMsg") String errMsg,
                          @Param("finishTime") Date finishTime);

    /**
     * 查询已过期的任务（仅取主键与文件路径，用于清理）
     *
     * @param expireBefore 过期阈值
     * @param limit 单次清理上限
     * @return 任务集合
     */
    public List<CrmExportTask> selectExpiredTasks(@Param("expireBefore") Date expireBefore,
                                                  @Param("limit") Integer limit);

    /**
     * 按主键批量删除
     *
     * @param ids 主键集合
     * @return 结果
     */
    public int deleteExportTaskByIds(@Param("ids") List<Long> ids);
}
