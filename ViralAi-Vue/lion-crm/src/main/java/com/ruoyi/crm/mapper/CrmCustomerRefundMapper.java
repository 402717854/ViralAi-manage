package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmCustomerRefund;
import com.ruoyi.crm.dto.CrmCustomerRefundDto;

/**
 * 客户退款单Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-20
 */
public interface CrmCustomerRefundMapper
{
    /**
     * 新增退款单
     *
     * @param refund 退款单
     * @return 结果
     */
    public int insertRefund(CrmCustomerRefund refund);

    /**
     * 退款单分页
     *
     * @param query 查询条件
     * @return 列表
     */
    public List<CrmCustomerRefundDto> selectRefundList(CrmCustomerRefundDto query);

    /**
     * 按ID查询实体
     *
     * @param id 主键
     * @return 退款单
     */
    public CrmCustomerRefund selectById(Long id);

    /**
     * 按ID查询并加行锁
     *
     * @param id 主键
     * @return 退款单
     */
    public CrmCustomerRefund selectByIdForUpdate(Long id);

    /**
     * 更新审批状态：仅当当前状态为 expectedStatus 时才更新
     *
     * @param id 主键
     * @param expectedStatus 期望的当前状态
     * @param status 目标状态
     * @param auditor 审批人
     * @param auditTime 审批时间
     * @param auditRemark 审批意见
     * @return 影响行数
     */
    public int updateStatus(@Param("id") Long id,
                            @Param("expectedStatus") Integer expectedStatus,
                            @Param("status") Integer status,
                            @Param("auditor") Long auditor,
                            @Param("auditTime") Date auditTime,
                            @Param("auditRemark") String auditRemark);
}
