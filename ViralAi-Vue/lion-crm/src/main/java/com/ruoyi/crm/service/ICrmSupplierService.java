package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.dto.CrmSupplierDto;
import com.ruoyi.crm.dto.CrmSupplierOptionDto;
import com.ruoyi.crm.dto.CrmSupplierProjectDto;
import com.ruoyi.crm.dto.CrmSupplierVo;

/**
 * CRM 供应商Service接口
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
public interface ICrmSupplierService 
{
    /**
     * 查询CRM 供应商详情（含产品关联列表）
     * 
     * @param id CRM 供应商主键
     * @return CRM 供应商详情，不存在时返回 null
     */
    public CrmSupplierDto selectCrmSupplierById(Long id);

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
     * 按供应商编号查询其产品关联明细
     * <p>
     * 返回模型ID、模型型号、模型厂商以及成本/售价/代理折扣与预计日消耗金额。
     *
     * @param supplierId 供应商编号
     * @return 供应商产品关联明细集合
     */
    public List<CrmSupplierProjectDto> selectSupplierProjectListBySupplierId(Long supplierId);

    /**
     * 新增CRM 供应商，并同步保存产品关联列表
     * 
     * @param crmSupplierVo CRM 供应商（含 projects 列表）
     * @return 结果
     */
    public int insertCrmSupplier(CrmSupplierVo crmSupplierVo);

    /**
     * 修改CRM 供应商，并按需替换产品关联列表
     * 
     * @param crmSupplierVo CRM 供应商（含 projects 列表，null 表示不涉及关联变更）
     * @return 结果
     */
    public int updateCrmSupplier(CrmSupplierVo crmSupplierVo);

    /**
     * 批量删除CRM 供应商
     * 
     * @param ids 需要删除的CRM 供应商主键集合
     * @return 结果
     */
    public int deleteCrmSupplierByIds(Long[] ids);

    /**
     * 删除CRM 供应商信息
     * 
     * @param id CRM 供应商主键
     * @return 结果
     */
    public int deleteCrmSupplierById(Long id);
}
