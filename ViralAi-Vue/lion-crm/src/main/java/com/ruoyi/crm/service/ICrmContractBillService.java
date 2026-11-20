package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmBillAuditVo;
import com.ruoyi.crm.dto.CrmBillDiffHandleVo;
import com.ruoyi.crm.dto.CrmBillGenerateResultVo;
import com.ruoyi.crm.dto.CrmBillGenerateVo;
import com.ruoyi.crm.dto.CrmBillSettlePreviewVo;
import com.ruoyi.crm.dto.CrmBillSettleResultVo;
import com.ruoyi.crm.dto.CrmBillSettleVo;
import com.ruoyi.crm.dto.CrmBillUsageItemDto;
import com.ruoyi.crm.dto.CrmContractBillDetailVo;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;
import com.ruoyi.crm.dto.CrmContractBillDto;
import com.ruoyi.crm.dto.CrmContractBillSubDto;

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
     * 查询子账单列表（内部应用渠道数据权限）
     *
     * <p>子账单按「父账单 + 自然日」拆分，供「子账单明细」二级页面使用；
     * 分页由调用方 {@code startPage()} 控制。</p>
     *
     * @param query 查询条件
     * @return 子账单列表
     */
    public List<CrmContractBillSubDto> selectCrmContractBillSubList(CrmContractBillSubDto query);

    /**
     * 查询账单详情：账单 + 差异 + 用量条数（用量明细另走分页接口）
     *
     * @param id 账单ID
     * @return 详情
     */
    public CrmContractBillDetailVo selectCrmContractBillDetail(Long id);

    /**
     * 校验账单存在且在当前账号的渠道数据权限内
     *
     * <p>供分页接口在 {@code startPage()} 之前完成校验——分页插件作用于
     * 第一条 SQL，校验语句必须先执行完毕。</p>
     *
     * @param id 账单ID
     */
    public void assertBillAccessible(Long id);

    /**
     * 查询账单归属的用量明细（分页由调用方 {@code startPage()} 控制）
     *
     * @param id 账单ID
     * @return 用量明细行
     */
    public List<CrmBillUsageItemDto> selectBillUsagePage(Long id);

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
     * 账单结算：已确认 → 已结算，按结算方式扣减客户余额或登记线下收款，
     * 并回写商机结算状态
     *
     * @param billId 账单ID
     * @param vo 结算请求（可为空，默认资金扣减）
     * @return 结算结果（含扣款前后余额）
     */
    public CrmBillSettleResultVo settle(Long billId, CrmBillSettleVo vo);

    /**
     * 结算预览：返回账单应付金额与客户可用余额，供结算弹窗预演
     *
     * @param billId 账单ID
     * @return 预览
     */
    public CrmBillSettlePreviewVo previewSettle(Long billId);

    /**
     * 结算撤销：已结算 → 已确认，红冲资金流水并退回余额
     *
     * @param billId 账单ID
     * @param remark 撤销原因
     */
    public void cancelSettle(Long billId, String remark);

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
