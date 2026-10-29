package com.ruoyi.crm.mapper;

import java.util.List;
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.dto.CrmSupplierDto;
import com.ruoyi.crm.dto.CrmSupplierOptionDto;

/**
 * CRM 供应商Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
public interface CrmSupplierMapper 
{
    /**
     * 查询CRM 供应商
     * 
     * @param id CRM 供应商主键
     * @return CRM 供应商
     */
    public CrmSupplier selectCrmSupplierById(Long id);

    /**
     * 查询CRM 供应商列表
     * 
     * @param crmSupplier CRM 供应商
     * @return CRM 供应商集合
     */
    public List<CrmSupplierDto> selectCrmSupplierList(CrmSupplier crmSupplier);

    /**
     * 查询所有可用状态的CRM 供应商选项
     *
     * @return CRM 供应商选项集合（仅 id、name）
     */
    public List<CrmSupplierOptionDto> selectAvailableSupplierOptions();

    /**
     * 新增CRM 供应商
     * 
     * @param crmSupplier CRM 供应商
     * @return 结果
     */
    public int insertCrmSupplier(CrmSupplier crmSupplier);

    /**
     * 修改CRM 供应商
     * 
     * @param crmSupplier CRM 供应商
     * @return 结果
     */
    public int updateCrmSupplier(CrmSupplier crmSupplier);

    /**
     * 删除CRM 供应商
     * 
     * @param id CRM 供应商主键
     * @return 结果
     */
    public int deleteCrmSupplierById(Long id);

    /**
     * 批量删除CRM 供应商
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCrmSupplierByIds(Long[] ids);
}
