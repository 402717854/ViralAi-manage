package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.CrmSupplierStatVo;
import com.ruoyi.crm.dto.CrmUsageBoardVo;
import com.ruoyi.crm.dto.CrmUsageReconcileResultVo;
import com.ruoyi.crm.dto.CrmUsageReconcileVo;

/**
 * 合同用量对账Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmContractUsageService
{
    /**
     * 查询台账列表（内部应用渠道数据权限）
     *
     * @param query 查询条件
     * @return 台账列表
     */
    public List<CrmContractUsageDto> selectCrmContractUsageList(CrmContractUsageDto query);

    /**
     * 查询台账详情
     *
     * @param id 主键
     * @return 台账
     */
    public CrmContractUsageDto selectCrmContractUsageById(Long id);

    /**
     * 执行对账：新流水入库生成台账 + 未匹配台账重试
     *
     * @param vo 区间与渠道
     * @return 执行结果
     */
    public CrmUsageReconcileResultVo reconcile(CrmUsageReconcileVo vo);

    /**
     * 单条重新匹配（补签合同后手工触发）
     *
     * @param usageId 台账ID
     * @return 1 匹配成功 0 仍失败
     */
    public int rematch(Long usageId);

    /**
     * 渠道对账看板
     *
     * @param billCycle 账期 yyyy-MM，为空取当前账期
     * @return 看板数据
     */
    public CrmUsageBoardVo board(String billCycle);

    /**
     * 渠道对账汇总
     *
     * @param billCycle 账期 yyyy-MM，为空取当前账期
     * @return 渠道汇总
     */
    public List<CrmSupplierStatVo> selectSupplierStat(String billCycle);
}
