package com.ruoyi.crm.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

/**
 * 对账执行结果
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmUsageReconcileResultVo
{
    /** 实际扫描的待匹配流水数 */
    private Integer scanned;

    /** 新生成台账条数 */
    private Integer created;

    /** 重新匹配的台账条数 */
    private Integer rematched;

    /** 匹配成功条数 */
    private Integer matched;

    /** 匹配失败条数 */
    private Integer abnormal;

    /** 新增差异记录条数 */
    private Integer diffCreated;

    /** 区间开始 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 区间结束 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
