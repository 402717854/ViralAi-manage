package com.ruoyi.crm.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.crm.service.ICrmContractService;

/**
 * 合同到期自动变更任务
 *
 * <p>由调度中心按 sys_job 配置每日凌晨执行，调用目标：{@code crmContractTask.expireContracts()}。
 * 处理逻辑：status = 已签约 且 expire_time &lt; 当前时间 的合同置为已到期，并写入操作日志。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component("crmContractTask")
public class CrmContractExpireTask
{
    private static final Logger log = LoggerFactory.getLogger(CrmContractExpireTask.class);

    @Autowired
    private ICrmContractService crmContractService;

    /**
     * 执行到期变更
     */
    public void expireContracts()
    {
        int count = crmContractService.expireContracts();
        log.info("合同到期自动变更完成，本次变更 {} 条", count);
    }
}
