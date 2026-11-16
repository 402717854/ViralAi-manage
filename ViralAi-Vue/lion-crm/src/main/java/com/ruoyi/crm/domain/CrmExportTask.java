package com.ruoyi.crm.domain;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * 异步导出任务对象 crm_export_task
 *
 * <p>流水与台账的导出改为「提交任务 → 后台生成 → 轮询 → 下载」：</p>
 * <ol>
 *   <li>{@code queryParams} 保存提交时的完整查询条件 JSON，其中已包含数据权限范围。
 *       异步线程拿不到登录上下文，所以权限必须在提交线程算好并固化，否则会退化成不过滤；</li>
 *   <li>文件落到 RuoYi 的 download 目录，下载统一走带鉴权的接口；</li>
 *   <li>任务与文件默认保留 7 天，提交新任务时顺带清理过期记录。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-11-15
 */
@Getter
@Setter
public class CrmExportTask
{
    /** 状态：待处理 */
    public static final int STATUS_PENDING = 0;

    /** 状态：处理中 */
    public static final int STATUS_RUNNING = 1;

    /** 状态：成功 */
    public static final int STATUS_SUCCESS = 2;

    /** 状态：失败 */
    public static final int STATUS_FAILED = 3;

    /** 主键 */
    private Long id;

    /** 业务类型：gateway-log 网关原始流水 / usage 对账台账 */
    private String bizType;

    /** 下载时展示的文件名（含扩展名） */
    private String fileName;

    /** 服务器落盘绝对路径 */
    private String filePath;

    /** 提交时的查询条件 JSON（含数据权限范围） */
    private String queryParams;

    /** 状态：0待处理 1处理中 2成功 3失败 */
    private Integer status;

    /** 导出行数 */
    private Integer rowCount;

    /** 失败原因 */
    private String errMsg;

    /** 提交人ID */
    private Long creatorId;

    /** 提交人账号 */
    private String creatorName;

    /** 提交时间 */
    private Date createTime;

    /** 开始执行时间 */
    private Date startTime;

    /** 完成时间 */
    private Date finishTime;

    /** 文件过期时间 */
    private Date expireTime;

    /** 是否删除 0否 1是 */
    private Integer deleted;
}
