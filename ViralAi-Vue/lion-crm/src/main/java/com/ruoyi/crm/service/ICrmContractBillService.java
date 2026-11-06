package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmBillAuditVo;
import com.ruoyi.crm.dto.CrmBillDiffHandleVo;
import com.ruoyi.crm.dto.CrmBillGenerateResultVo;
import com.ruoyi.crm.dto.CrmBillGenerateVo;
import com.ruoyi.crm.dto.CrmContractBillDetailVo;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;
import com.ruoyi.crm.dto.CrmContractBillDto;

/**
 * 对账账单Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmContractBillService
{
    /**
     * 查询账单列表（内部应用渠道数据权限）
     *
     * @param query 查询条件
     * @return 账单列表
     */
    public List<CrmContractBillDto> selectCrmContractBillList(CrmContractBillDto query);

    /**
     * 查询账单详情：账单 + 归属用量 + 同渠道同账期差异
     *
     * @param id 账单ID
     * @return 详情
     */
    public CrmContractBillDetailVo selectCrmContractBillDetail(Long id);

    /**
     * 生成账单（按渠道 + 账期，合同维度分组）
     *
     * @param vo 渠道与账期
     * @return 生成结果
     */
    public CrmBillGenerateResultVo generate(CrmBillGenerateVo vo);

    /**
     * 账单复核：确认或差异挂起
     *
     * @param vo 复核参数
     * @return 结果
     */
    public int audit(CrmBillAuditVo vo);

    /**
     * 账单结算：已确认 → 已结算，并回写商机结算状态
     *
     * @param billId 账单ID
     * @return 结果
     */
    public int settle(Long billId);

    /**
     * 查询差异列表（内部应用渠道数据权限）
     *
     * @param query 查询条件
     * @return 差异列表
     */
    public List<CrmContractBillDiffDto> selectDiffList(CrmContractBillDiffDto query);

    /**
     * 处理差异：调账 / 豁免 / 驳回
     *
     * @param vo 处理参数
     * @return 结果
     */
    public int handleDiff(CrmBillDiffHandleVo vo);

    /**
     * 按月批量生成全部渠道账单（定时任务用）
     *
     * @param billCycle 账期 yyyy-MM
     * @return 生成的账单数
     */
    public int generateMonthlyBills(String billCycle);
}
