package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmSupplierProject;

/**
 * 供应商产品关联Service接口
 * 
 * @author ruoyi
 * @date 2026-10-29
 */
public interface ICrmSupplierProjectService 
{
    /**
     * 查询供应商产品关联
     * 
     * @param id 供应商产品关联主键
     * @return 供应商产品关联
     */
    public CrmSupplierProject selectCrmSupplierProjectById(Long id);

    /**
     * 查询供应商产品关联列表
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 供应商产品关联集合
     */
    public List<CrmSupplierProject> selectCrmSupplierProjectList(CrmSupplierProject crmSupplierProject);

    /**
     * 新增供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    public int insertCrmSupplierProject(CrmSupplierProject crmSupplierProject);

    /**
     * 修改供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    public int updateCrmSupplierProject(CrmSupplierProject crmSupplierProject);

    /**
     * 批量删除供应商产品关联
     * 
     * @param ids 需要删除的供应商产品关联主键集合
     * @return 结果
     */
    public int deleteCrmSupplierProjectByIds(Long[] ids);

    /**
     * 删除供应商产品关联信息
     * 
     * @param id 供应商产品关联主键
     * @return 结果
     */
    public int deleteCrmSupplierProjectById(Long id);
}
