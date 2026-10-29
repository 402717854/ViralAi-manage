package com.ruoyi.crm.mapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.dto.CrmBusinessDto;

/**
 * CRM 商机Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
public interface CrmBusinessMapper 
{
    /**
     * 查询CRM 商机
     * 
     * @param id CRM 商机主键
     * @return CRM 商机
     */
    public CrmBusiness selectCrmBusinessById(Long id);

    /**
     * 查询CRM 商机列表
     * 
     * @param crmBusiness CRM 商机
     * @return CRM 商机集合
     */
    public List<CrmBusinessDto> selectCrmBusinessList(CrmBusiness crmBusiness);

    /**
     * 新增CRM 商机
     * 
     * @param crmBusiness CRM 商机
     * @return 结果
     */
    public int insertCrmBusiness(CrmBusiness crmBusiness);

    /**
     * 修改CRM 商机
     * 
     * @param crmBusiness CRM 商机
     * @return 结果
     */
    public int updateCrmBusiness(CrmBusiness crmBusiness);

    /**
     * 删除CRM 商机
     * 
     * @param id CRM 商机主键
     * @return 结果
     */
    public int deleteCrmBusinessById(Long id);

    /**
     * 批量删除CRM 商机
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCrmBusinessByIds(Long[] ids);

    /**
     * 更新商机状态（仅更新状态相关字段，以当前状态为条件做并发保护）
     *
     * @param id 商机主键
     * @param oldStatus 流转前的状态，用于乐观并发校验
     * @param busStatus 目标状态
     * @param failRemark 中断备注，传 null 表示清空
     * @param dealTime 成交日期，传 null 表示保持不变
     * @param creditAmount 授信金额，传 null 表示保持不变
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
}
