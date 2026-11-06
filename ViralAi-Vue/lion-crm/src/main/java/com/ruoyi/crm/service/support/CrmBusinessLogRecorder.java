package com.ruoyi.crm.service.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmBusinessLog;
import com.ruoyi.crm.enums.BusinessActionEnum;
import com.ruoyi.crm.mapper.CrmBusinessLogMapper;

/**
 * 商机操作日志写入器
 *
 * <p>商机主表服务、产品明细服务与合同模块（回写签约状态）共用，避免各处重复拼装日志对象。
 * 内容统一截断到 1000 字符，与 crm_business_log.content 字段长度一致。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component
public class CrmBusinessLogRecorder
{
    /** 操作内容最大长度 */
    private static final int CONTENT_MAX_LENGTH = 1000;

    @Autowired
    private CrmBusinessLogMapper crmBusinessLogMapper;

    /**
     * 写入当前登录人操作日志
     *
     * @param busId 商机ID
     * @param action 操作类型
     * @param fromStatus 变更前状态，可为 null
     * @param toStatus 变更后状态，可为 null
     * @param content 操作内容
     */
    public void record(Long busId, BusinessActionEnum action, Integer fromStatus, Integer toStatus, String content)
    {
        CrmBusinessLog log = new CrmBusinessLog();
        log.setBusId(busId);
        log.setAction(action.getCode());
        log.setActionName(action.getName());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setContent(StringUtils.substring(content, 0, CONTENT_MAX_LENGTH));
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setCreateTime(DateUtils.getNowDate());
        crmBusinessLogMapper.insertCrmBusinessLog(log);
    }

    /**
     * 写入系统操作日志（无登录上下文的场景）
     *
     * @param busId 商机ID
     * @param action 操作类型
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param content 操作内容
     * @param operatorName 操作人名称
     */
    public void recordSystem(Long busId, BusinessActionEnum action, Integer fromStatus, Integer toStatus,
                             String content, String operatorName)
    {
        CrmBusinessLog log = new CrmBusinessLog();
        log.setBusId(busId);
        log.setAction(action.getCode());
        log.setActionName(action.getName());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setContent(StringUtils.substring(content, 0, CONTENT_MAX_LENGTH));
        log.setOperatorName(operatorName);
        log.setCreateTime(DateUtils.getNowDate());
        crmBusinessLogMapper.insertCrmBusinessLog(log);
    }
}
