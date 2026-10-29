package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.dto.CrmBusStatusOptionDto;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;

/**
 * CRM 商机Service接口
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
public interface ICrmBusinessService 
{
    /**
     * 查询CRM 商机
     * 
     * @param id CRM 商机主键
     * @return CRM 商机
     */
    public CrmBusinessDto selectCrmBusinessById(Long id);

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
    public int insertCrmBusiness(CrmBusinessVo crmBusiness);

    /**
     * 修改CRM 商机
     * 
     * @param crmBusiness CRM 商机
     * @return 结果
     */
    public int updateCrmBusiness(CrmBusinessVo crmBusinessVo);

    /**
     * 批量删除CRM 商机
     * 
     * @param ids 需要删除的CRM 商机主键集合
     * @return 结果
     */
    public int deleteCrmBusinessByIds(Long[] ids);

    /**
     * 删除CRM 商机信息
     * 
     * @param id CRM 商机主键
     * @return 结果
     */
    public int deleteCrmBusinessById(Long id);

    /**
     * 商机状态流转
     *
     * @param statusVo 状态流转请求（商机编号、目标状态、中断备注、成交日期）
     * @return 结果
     */
    public int changeBusStatus(CrmBusinessStatusVo statusVo);

    /**
     * 查询商机当前状态下允许流转到的目标状态
     *
     * @param id 商机主键
     * @return 目标状态选项集合
     */
    public List<CrmBusStatusOptionDto> selectAllowedNextStatus(Long id);
}
