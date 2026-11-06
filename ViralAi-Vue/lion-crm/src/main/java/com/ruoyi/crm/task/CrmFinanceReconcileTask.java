package com.ruoyi.crm.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ruoyi.crm.dto.CrmUsageReconcileResultVo;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;
import com.ruoyi.crm.service.ICrmContractBillService;
import com.ruoyi.crm.service.ICrmContractUsageService;
import com.ruoyi.crm.service.IGatewayCallLogService;
import com.ruoyi.crm.service.support.CrmBillCycleUtils;

/**
 * 网关用量对账定时任务
 *
 * <p>sys_job 配置的调用目标：</p>
 * <ul>
 *   <li>{@code crmFinanceTask.reconcileYesterday()}：每日 02:10，先尝试从网关拉取昨日流水
 *       （未配置 crm.gateway.pull-url 时跳过），再对昨日区间执行对账；</li>
 *   <li>{@code crmFinanceTask.generateLastMonthBill()}：每月 1 日 03:10，生成上一账期全部渠道账单。</li>
 * </ul>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Component("crmFinanceTask")
public class CrmFinanceReconcileTask
{
    private static final Logger log = LoggerFactory.getLogger(CrmFinanceReconcileTask.class);

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Autowired
    private IGatewayCallLogService gatewayCallLogService;

    @Autowired
    private ICrmContractUsageService crmContractUsageService;

    @Autowired
    private ICrmContractBillService crmContractBillService;

    /**
     * 对账昨日流水
     */
    public void reconcileYesterday()
    {
        Date yesterday = CrmBillCycleUtils.yesterday();
        SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_PATTERN);
        String start = format.format(CrmBillCycleUtils.parseStart(format.format(yesterday)));
        String end = format.format(CrmBillCycleUtils.parseEnd(format.format(yesterday)));

        try
        {
            // 网关拉取为可选能力：未配置地址时会抛业务异常，这里降级为「仅对账已上报流水」
            gatewayCallLogService.pullAndIngest(start, end, null);
        }
        catch (Exception e)
        {
            log.warn("网关流水拉取未执行（可忽略，若使用上报模式属正常）：{}", e.getMessage());
        }

        CrmUsageReconcileVo vo = new CrmUsageReconcileVo();
        vo.setStartTime(start);
        vo.setEndTime(end);
        vo.setRetryAbnormal(Boolean.TRUE);

        CrmUsageReconcileResultVo result = crmContractUsageService.reconcile(vo);
        log.info("网关对账完成，区间 {} ~ {}，扫描 {} 条，新增台账 {} 条，重试 {} 条，成功 {} 条，异常 {} 条，新增差异 {} 条",
                start, end, result.getScanned(), result.getCreated(), result.getRematched(),
                result.getMatched(), result.getAbnormal(), result.getDiffCreated());
    }

    /**
     * 生成上一账期账单
     */
    public void generateLastMonthBill()
    {
        String cycle = CrmBillCycleUtils.lastCycle();
        int count = crmContractBillService.generateMonthlyBills(cycle);
        log.info("账期 {} 账单生成完成，共生成 {} 张账单", cycle, count);
    }
}
