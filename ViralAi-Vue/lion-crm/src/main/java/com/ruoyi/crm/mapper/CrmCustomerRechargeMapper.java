package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmCustomerRecharge;
import com.ruoyi.crm.dto.CrmCustomerRechargeDto;

/**
 * 客户充值单Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmCustomerRechargeMapper
{
    /**
     * 新增充值单
     *
     * @param recharge 充值单
     * @return 结果
     */
    public int insertRecharge(CrmCustomerRecharge recharge);

    /**
     * 充值单分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerRechargeDto> selectRechargeList(CrmCustomerRechargeDto query);

    /**
     * 按ID查询实体
     *
     * @param id 主键
     * @return 充值单
     */
    public CrmCustomerRecharge selectById(Long id);

    /**
     * 按ID查询并加行锁（审核时防并发重复入账）
     *
     * @param id 主键
     * @return 充值单
     */
    public CrmCustomerRecharge selectByIdForUpdate(Long id);

    /**
     * 更新审核状态：仅当当前状态为 expectedStatus 时才更新，天然防重复审核
     *
     * @param id 主键
     * @param expectedStatus 期望的当前状态
     * @param status 目标状态
     * @param auditor 审核人
     * @param auditTime 审核时间
     * @param auditRemark 审核意见
     * @return 影响行数，0 表示状态已变更
     */
    public int updateStatus(@Param("id") Long id,
                            @Param("expectedStatus") Integer expectedStatus,
                            @Param("status") Integer status,
                            @Param("auditor") Long auditor,
                            @Param("auditTime") Date auditTime,
                            @Param("auditRemark") String auditRemark);

    /**
     * 统计待审核充值单数（首页待办）
     *
     * @param ownerUser 数据权限负责人，为空不限
     * @return 条数
     */
    public int countPending(@Param("ownerUser") String ownerUser);
}
