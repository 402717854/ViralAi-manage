package com.ruoyi.crm.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmCustomerAccount;
import com.ruoyi.crm.dto.CrmCustomerAccountDto;
import com.ruoyi.crm.dto.CrmFundSummaryVo;

/**
 * 客户资金账户Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmCustomerAccountMapper
{
    /**
     * 账户列表：以客户为主表左连账户，保证无余额客户也能查到（余额显示 0）
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerAccountDto> selectAccountList(CrmCustomerAccountDto query);

    /**
     * 账户详情（含客户名称、负责人）
     *
     * @param customerId 客户ID
     * @return 详情
     */
    public CrmCustomerAccountDto selectAccountDetail(@Param("customerId") Long customerId);

    /**
     * 按客户查询账户
     *
     * @param customerId 客户ID
     * @return 账户，不存在返回 null
     */
    public CrmCustomerAccount selectByCustomerId(@Param("customerId") Long customerId);

    /**
     * 按客户查询账户并加行锁（所有余额变动的入口）
     *
     * @param customerId 客户ID
     * @return 账户
     */
    public CrmCustomerAccount selectByCustomerIdForUpdate(@Param("customerId") Long customerId);

    /**
     * 新增账户
     *
     * @param account 账户
     * @return 结果
     */
    public int insertAccount(CrmCustomerAccount account);

    /**
     * 更新余额与累计项（乐观锁：version 不匹配则不更新）
     *
     * @param id 账户ID
     * @param balance 新余额
     * @param totalRecharge 累计充值
     * @param totalDeduct 累计扣减
     * @param totalRefund 累计退款
     * @param lastFlowTime 最近变动时间
     * @param version 期望版本号
     * @return 影响行数，0 表示版本冲突
     */
    public int updateAccountBalance(@Param("id") Long id,
                                    @Param("balance") BigDecimal balance,
                                    @Param("totalRecharge") BigDecimal totalRecharge,
                                    @Param("totalDeduct") BigDecimal totalDeduct,
                                    @Param("totalRefund") BigDecimal totalRefund,
                                    @Param("lastFlowTime") Date lastFlowTime,
                                    @Param("version") Integer version);

    /**
     * 资金汇总（列表页顶部）
     *
     * @param monthStart 本月起始时间
     * @param ownerUser 数据权限负责人（为空不限）
     * @return 汇总
     */
    public CrmFundSummaryVo selectFundSummary(@Param("monthStart") Date monthStart,
                                              @Param("ownerUser") String ownerUser);

    /**
     * 查询全部账户（日终校验用）
     *
     * @return 账户列表
     */
    public List<CrmCustomerAccount> selectAllAccounts();
}
