package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.domain.CrmCustomerAccountFlow;
import com.ruoyi.crm.dto.CrmCustomerAccountDto;
import com.ruoyi.crm.dto.CrmCustomerAccountFlowDto;
import com.ruoyi.crm.dto.CrmCustomerRechargeDto;
import com.ruoyi.crm.dto.CrmCustomerRefundDto;
import com.ruoyi.crm.dto.CrmFundAdjustVo;
import com.ruoyi.crm.dto.CrmFundAuditVo;
import com.ruoyi.crm.dto.CrmFundCheckVo;
import com.ruoyi.crm.dto.CrmFundInitVo;
import com.ruoyi.crm.dto.CrmFundRechargeVo;
import com.ruoyi.crm.dto.CrmFundRefundVo;
import com.ruoyi.crm.dto.CrmFundSummaryVo;

/**
 * 客户资金Service接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface ICrmFundService
{
    /**
     * 客户资金列表（以客户为主表，无余额客户同样返回）
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerAccountDto> selectAccountList(CrmCustomerAccountDto query);

    /**
     * 账户详情
     *
     * @param customerId 客户ID
     * @return 详情
     */
    public CrmCustomerAccountDto selectAccountDetail(Long customerId);

    /**
     * 资金汇总（列表页顶部）
     *
     * @return 汇总
     */
    public CrmFundSummaryVo selectSummary();

    /**
     * 流水分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerAccountFlowDto> selectFlowList(CrmCustomerAccountFlowDto query);

    /**
     * 充值单分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerRechargeDto> selectRechargeList(CrmCustomerRechargeDto query);

    /**
     * 退款单分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerRefundDto> selectRefundList(CrmCustomerRefundDto query);

    /**
     * 充值登记
     *
     * @param vo 请求
     * @return 充值单号
     */
    public String createRecharge(CrmFundRechargeVo vo);

    /**
     * 充值审核（通过即入账写流水）
     *
     * @param vo 审核请求
     */
    public void auditRecharge(CrmFundAuditVo vo);

    /**
     * 退款申请
     *
     * @param vo 请求
     * @return 退款单号
     */
    public String createRefund(CrmFundRefundVo vo);

    /**
     * 退款审批（通过即扣减余额并写流水）
     *
     * @param vo 审批请求
     */
    public void auditRefund(CrmFundAuditVo vo);

    /**
     * 人工调账
     *
     * @param vo 请求
     * @return 生成的资金流水
     */
    public CrmCustomerAccountFlow adjust(CrmFundAdjustVo vo);

    /**
     * 期初余额批量导入
     *
     * @param vo 导入请求
     * @return 成功导入笔数
     */
    public int importInitBalance(CrmFundInitVo vo);

    /**
     * 账实一致性校验（日终任务与手工排查共用）
     *
     * @return 不一致的账户清单，空表示全部一致
     */
    public List<CrmFundCheckVo> checkConsistency();

    /**
     * 待审核充值单数（首页待办）
     *
     * @return 条数
     */
    public int countPendingRecharge();
}
