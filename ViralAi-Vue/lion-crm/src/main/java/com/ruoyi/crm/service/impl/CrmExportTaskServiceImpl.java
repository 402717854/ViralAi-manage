package com.ruoyi.crm.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.crm.domain.CrmExportTask;
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.mapper.CrmContractUsageMapper;
import com.ruoyi.crm.mapper.CrmExportTaskMapper;
import com.ruoyi.crm.mapper.GatewayCallLogMapper;
import com.ruoyi.crm.service.ICrmExportTaskService;
import com.ruoyi.crm.service.support.CrmFinanceDataScope;

/**
 * 异步导出任务Service实现
 *
 * <p>关键约束：</p>
 * <ol>
 *   <li>查询条件（含数据权限范围）在提交线程固化后写入任务表。异步线程没有
 *       SecurityContext，如果在那里才解析数据权限，会因「无登录上下文」而退化成不过滤；</li>
 *   <li>异步执行直接调用 Mapper，绕开 Service 里的 applyScope，保证只使用固化后的范围；</li>
 *   <li>文件落在 RuoYi 的 download 目录，对外只暴露带鉴权的下载接口，不做匿名静态访问；</li>
 *   <li>过期任务（默认 7 天）在每次提交时顺带清理，避免磁盘无限增长。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
@Service
public class CrmExportTaskServiceImpl implements ICrmExportTaskService
{
    private static final Logger log = LoggerFactory.getLogger(CrmExportTaskServiceImpl.class);

    /** 文件保留天数 */
    private static final int EXPIRE_DAYS = 7;

    /** 单次清理的过期任务上限 */
    private static final int MAX_CLEAN_PER_RUN = 200;

    /** err_msg 字段长度 */
    private static final int MAX_ERR_MSG = 900;

    /** Excel 内容类型 */
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private CrmExportTaskMapper exportTaskMapper;

    @Autowired
    private GatewayCallLogMapper gatewayCallLogMapper;

    @Autowired
    private CrmContractUsageMapper crmContractUsageMapper;

    @Autowired
    private CrmFinanceDataScope crmFinanceDataScope;

    @Autowired
    @Qualifier("threadPoolTaskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public Long submitGatewayLogExport(GatewayCallLogDto query)
    {
        GatewayCallLogDto effective = query == null ? new GatewayCallLogDto() : query;
        // 提交线程里把数据权限范围固化进查询条件，异步线程直接用
        crmFinanceDataScope.applyScope(effective);
        return submit(BIZ_GATEWAY_LOG, effective, "网关调用流水");
    }

    @Override
    public Long submitUsageExport(CrmContractUsageDto query)
    {
        CrmContractUsageDto effective = query == null ? new CrmContractUsageDto() : query;
        crmFinanceDataScope.applyScope(effective);
        return submit(BIZ_USAGE, effective, "对账台账");
    }

    @Override
    public CrmExportTask selectTask(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("导出任务不存在");
        }
        CrmExportTask task = exportTaskMapper.selectExportTaskById(id);
        if (task == null)
        {
            throw new ServiceException("导出任务不存在或已被清理，请重新导出");
        }
        Long userId = SecurityUtils.getUserId();
        if (!SecurityUtils.isAdmin() && userId != null && !userId.equals(task.getCreatorId()))
        {
            throw new ServiceException("无权访问他人的导出任务");
        }
        return task;
    }

    @Override
    public void download(Long id, HttpServletResponse response)
    {
        CrmExportTask task = selectTask(id);
        if (task.getStatus() == null || task.getStatus() != CrmExportTask.STATUS_SUCCESS)
        {
            throw new ServiceException("导出任务尚未完成，请稍后再试");
        }
        if (StringUtils.isEmpty(task.getFilePath()))
        {
            throw new ServiceException("导出文件不存在，请重新提交导出");
        }
        File file = new File(task.getFilePath());
        if (!file.exists())
        {
            throw new ServiceException("导出文件已过期（保留 " + EXPIRE_DAYS + " 天），请重新提交导出");
        }
        try (InputStream in = new FileInputStream(file))
        {
            response.reset();
            response.setContentType(XLSX_CONTENT_TYPE);
            response.setCharacterEncoding("utf-8");
            response.setContentLengthLong(file.length());
            String encoded = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encoded);
            IOUtils.copy(in, response.getOutputStream());
            response.getOutputStream().flush();
        }
        catch (IOException e)
        {
            log.error("导出文件下载失败，任务={}", id, e);
            throw new ServiceException("导出文件下载失败，请稍后重试");
        }
    }

    /**
     * 建任务并提交到线程池
     *
     * @param bizType 业务类型
     * @param query 已固化权限的查询条件
     * @param title 文件标题（用于生成下载文件名）
     * @return 任务ID
     */
    private Long submit(String bizType, Object query, String title)
    {
        cleanExpiredTasks();
        Date now = new Date();
        CrmExportTask task = new CrmExportTask();
        task.setBizType(bizType);
        task.setFileName(title + "_" + DateUtils.parseDateToStr("yyyyMMddHHmmss", now) + ".xlsx");
        task.setQueryParams(JSON.toJSONString(query));
        task.setStatus(CrmExportTask.STATUS_PENDING);
        task.setCreatorId(SecurityUtils.getUserId());
        task.setCreatorName(SecurityUtils.getUsername());
        task.setCreateTime(now);
        task.setExpireTime(new Date(now.getTime() + EXPIRE_DAYS * 24L * 60L * 60L * 1000L));
        exportTaskMapper.insertExportTask(task);
        Long taskId = task.getId();
        // 线程池拒绝策略为 CallerRuns，队列满时退化为同步执行，结果仍会落到任务上
        threadPoolTaskExecutor.execute(() -> run(taskId));
        log.info("导出任务已提交，任务={}，类型={}，提交人={}", taskId, bizType, task.getCreatorName());
        return taskId;
    }

    /**
     * 执行任务：失败只记录状态，不向外抛，避免线程池吞掉异常后任务永远停在处理中
     *
     * @param taskId 任务ID
     */
    private void run(Long taskId)
    {
        CrmExportTask task = exportTaskMapper.selectExportTaskById(taskId);
        if (task == null)
        {
            return;
        }
        long begin = System.currentTimeMillis();
        try
        {
            exportTaskMapper.markRunning(taskId, new Date());
            ExportFile file = generate(task);
            exportTaskMapper.markSuccess(taskId, file.path, file.rows, new Date());
            log.info("导出任务完成，任务={}，行数={}，耗时={}ms", taskId, file.rows, System.currentTimeMillis() - begin);
        }
        catch (Throwable e)
        {
            log.error("导出任务失败，任务={}", taskId, e);
            String msg = StringUtils.isEmpty(e.getMessage()) ? e.getClass().getSimpleName() : e.getMessage();
            exportTaskMapper.markFailed(taskId, StringUtils.substring(msg, 0, MAX_ERR_MSG), new Date());
        }
    }

    /**
     * 按业务类型生成 Excel，落到 RuoYi 的 download 目录
     *
     * @param task 任务
     * @return 文件路径与行数
     */
    private ExportFile generate(CrmExportTask task)
    {
        if (BIZ_GATEWAY_LOG.equals(task.getBizType()))
        {
            GatewayCallLogDto query = JSON.parseObject(task.getQueryParams(), GatewayCallLogDto.class);
            List<GatewayCallLogDto> list = gatewayCallLogMapper
                    .selectGatewayCallLogList(query == null ? new GatewayCallLogDto() : query);
            return writeExcel(new ExcelUtil<GatewayCallLogDto>(GatewayCallLogDto.class), list, "网关调用流水");
        }
        if (BIZ_USAGE.equals(task.getBizType()))
        {
            CrmContractUsageDto query = JSON.parseObject(task.getQueryParams(), CrmContractUsageDto.class);
            List<CrmContractUsageDto> list = crmContractUsageMapper
                    .selectCrmContractUsageList(query == null ? new CrmContractUsageDto() : query);
            return writeExcel(new ExcelUtil<CrmContractUsageDto>(CrmContractUsageDto.class), list, "对账台账");
        }
        throw new ServiceException("未知的导出业务类型：" + task.getBizType());
    }

    /**
     * 写 Excel 文件
     *
     * @param util Excel 工具
     * @param list 数据
     * @param sheetName 工作表名（同时用于文件名）
     * @param <T> 行类型
     * @return 文件路径与行数
     */
    private <T> ExportFile writeExcel(ExcelUtil<T> util, List<T> list, String sheetName)
    {
        List<T> rows = list == null ? new ArrayList<>() : list;
        AjaxResult result = util.exportExcel(rows, sheetName);
        Object data = result == null ? null : result.get(AjaxResult.DATA_TAG);
        String storedName = data == null ? null : String.valueOf(data);
        if (StringUtils.isEmpty(storedName))
        {
            throw new ServiceException("导出文件生成失败，请稍后重试");
        }
        return new ExportFile(RuoYiConfig.getDownloadPath() + storedName, rows.size());
    }

    /**
     * 清理过期任务与文件（每次提交时顺带执行，失败不影响提交）
     */
    private void cleanExpiredTasks()
    {
        try
        {
            List<CrmExportTask> expired = exportTaskMapper.selectExpiredTasks(new Date(), MAX_CLEAN_PER_RUN);
            if (expired == null || expired.isEmpty())
            {
                return;
            }
            List<Long> ids = new ArrayList<>(expired.size());
            for (CrmExportTask item : expired)
            {
                if (StringUtils.isNotEmpty(item.getFilePath()))
                {
                    try
                    {
                        Files.deleteIfExists(Paths.get(item.getFilePath()));
                    }
                    catch (Exception e)
                    {
                        log.warn("删除过期导出文件失败：{}", item.getFilePath(), e);
                    }
                }
                ids.add(item.getId());
            }
            exportTaskMapper.deleteExportTaskByIds(ids);
            log.info("清理过期导出任务 {} 条", ids.size());
        }
        catch (Exception e)
        {
            log.warn("清理过期导出任务失败", e);
        }
    }

    /** 生成结果：落盘路径与导出行数 */
    private static final class ExportFile
    {
        private final String path;

        private final int rows;

        private ExportFile(String path, int rows)
        {
            this.path = path;
            this.rows = rows;
        }
    }
}
