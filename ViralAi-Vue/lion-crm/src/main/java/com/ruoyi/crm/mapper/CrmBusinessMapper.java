package com.ruoyi.crm.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.dto.CrmBusinessContractVo;
import com.ruoyi.crm.dto.CrmBusinessDetailVo;
import com.ruoyi.crm.dto.CrmBusinessDto;

/**
 * CRM 商机Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmBusinessMapper
{
    /**
     * 查询商机（主表原始字段）
     *
     * @param id 商机主键
     * @return 商机
     */
    public CrmBusiness selectCrmBusinessById(Long id);

    /**
     * 查询商机并加行锁，用于转合同等需要防并发重复提交的场景
     *
     * @param id 商机主键
     * @return 商机
     */
    public CrmBusiness selectCrmBusinessByIdForUpdate(Long id);

    /**
     * 查询商机详情（含客户名称、渠道名称、操作人名称、合同标记）
     *
     * @param id 商机主键
     * @return 详情
     */
    public CrmBusinessDetailVo selectCrmBusinessDetail(Long id);

    /**
     * 查询商机列表
     *
     * @param query 查询条件（含 params 内的数据权限与成交时间区间）
     * @return 商机集合
     */
    public List<CrmBusinessDto> selectCrmBusinessList(CrmBusinessDto query);

    /**
     * 新增商机
     *
     * @param crmBusiness 商机
     * @return 结果
     */
    public int insertCrmBusiness(CrmBusiness crmBusiness);

    /**
     * 修改商机基础信息（仅白名单字段）
     *
     * @param crmBusiness 商机
     * @return 结果
     */
    public int updateCrmBusiness(CrmBusiness crmBusiness);

    /**
     * 更新商机状态（以当前状态为条件做并发保护）
     *
     * @param id 商机主键
     * @param oldStatus 流转前状态
     * @param busStatus 目标状态
     * @param failRemark 中断备注，非中断流转时置空
     * @param dealTime 成交时间，null 表示保持不变
     * @param creditAmount 授信额度，null 表示保持不变
     * @param updater 更新人
     * @return 影响行数，0 表示状态已被其他操作变更
     */
    public int updateBusStatus(@Param("id") Long id,
                               @Param("oldStatus") Long oldStatus,
                               @Param("busStatus") Long busStatus,
                               @Param("failRemark") String failRemark,
                               @Param("dealTime") Date dealTime,
                               @Param("creditAmount") BigDecimal creditAmount,
                               @Param("updater") Long updater);

    /**
     * 回填结算状态
     *
     * @param id 商机主键
     * @param settleStatus 结算状态 0未结算 1已结算
     * @param updater 更新人
     * @return 结果
     */
    public int updateSettleStatus(@Param("id") Long id,
                                  @Param("settleStatus") Integer settleStatus,
                                  @Param("updater") Long updater);

    /**
     * 回写合同签署状态（合同审批通过/作废时由合同模块调用）
     *
     * @param busId 商机主键
     * @param contactStatus 合同状态 0未签 1已签
     * @return 结果
     */
    public int updateContactStatus(@Param("busId") Long busId,
                                   @Param("contactStatus") Integer contactStatus);

    /**
     * 逻辑删除商机
     *
     * @param ids 需要删除的商机主键集合
     * @return 结果
     */
    public int deleteCrmBusinessByIds(Long[] ids);

    /**
     * 查询指定账号负责的客户ID集合（数据权限：客户负责人）
     *
     * @param ownerUser 登录账号
     * @return 客户ID集合
     */
    public List<Long> selectOwnedCustomerIds(@Param("ownerUser") String ownerUser);

    /**
     * 查询商机关联的合同列表
     *
     * @param busId 商机主键
     * @return 合同集合
     */
    public List<CrmBusinessContractVo> selectContractsByBusId(@Param("busId") Long busId);

    /**
     * 统计商机已生成的合同数量（不含已删除），用于转合同幂等校验
     *
     * @param busId 商机主键
     * @return 合同数量
     */
    public int countContractsByBusId(@Param("busId") Long busId);
}
