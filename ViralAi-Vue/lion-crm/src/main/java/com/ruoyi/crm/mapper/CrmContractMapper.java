package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.dto.CrmContractDto;

/**
 * 合同Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractMapper
{
    /**
     * 查询合同（不加关联，用于状态校验）
     *
     * @param id 合同主键
     * @return 合同
     */
    public CrmContract selectCrmContractById(Long id);

    /**
     * 查询合同详情（含客户、渠道、商机、创建人名称）
     *
     * @param id 合同主键
     * @return 合同详情
     */
    public CrmContractDto selectCrmContractDetail(Long id);

    /**
     * 查询合同列表
     *
     * @param query 查询条件（含 params 数据权限与时间范围）
     * @return 合同集合
     */
    public List<CrmContractDto> selectCrmContractList(CrmContractDto query);

    /**
     * 新增合同
     *
     * @param crmContract 合同
     * @return 结果
     */
    public int insertCrmContract(CrmContract crmContract);

    /**
     * 修改合同主信息（不含状态，状态变更走 updateContractStatus）
     *
     * @param crmContract 合同
     * @return 结果
     */
    public int updateCrmContract(CrmContract crmContract);

    /**
     * 按当前状态条件更新合同状态，用于防止并发状态覆盖
     *
     * @param id 合同主键
     * @param fromStatus 期望的当前状态
     * @param toStatus 目标状态
     * @param auditTime 审批时间
     * @param signTime 签约时间
     * @param auditOpinion 审批意见
     * @param cancelTime 作废时间
     * @param cancelReason 作废原因
     * @param archiveTime 归档时间
     * @param updater 更新者
     * @param updateTime 更新时间
     * @return 影响行数，0 表示状态已被其他操作变更
     */
    public int updateContractStatus(@Param("id") Long id,
                                    @Param("fromStatus") Integer fromStatus,
                                    @Param("toStatus") Integer toStatus,
                                    @Param("auditTime") Date auditTime,
                                    @Param("signTime") Date signTime,
                                    @Param("auditOpinion") String auditOpinion,
                                    @Param("cancelTime") Date cancelTime,
                                    @Param("cancelReason") String cancelReason,
                                    @Param("archiveTime") Date archiveTime,
                                    @Param("updater") Long updater,
                                    @Param("updateTime") Date updateTime);

    /**
     * 删除合同（逻辑删除）
     *
     * @param id 合同主键
     * @return 结果
     */
    public int deleteCrmContractById(Long id);

    /**
     * 批量删除合同（逻辑删除）
     *
     * @param ids 合同主键集合
     * @return 结果
     */
    public int deleteCrmContractByIds(Long[] ids);

    /**
     * 查询指定账号作为负责人的渠道ID集合（财务对账数据权限仍按渠道收敛）
     *
     * @return 渠道ID集合
     */
    public List<Long> selectAllSupplierIds();

    /**
     * 查询指定账号作为负责人的客户ID集合（合同数据权限按客户收敛）
     *
     * @param ownerUser 登录账号
     * @return 客户ID集合
     */
    public List<Long> selectOwnedCustomerIds(String ownerUser);

    /**
     * 查询已过到期时间但仍为已签约状态的合同
     *
     * @param now 当前时间
     * @return 合同集合
     */
    public List<CrmContract> selectExpiredContracts(@Param("now") Date now);

    /**
     * 将单条合同置为已到期
     *
     * @param id 合同主键
     * @param now 变更时间
     * @return 结果
     */
    public int updateExpiredContract(@Param("id") Long id, @Param("now") Date now);
}
