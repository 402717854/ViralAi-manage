package com.ruoyi.crm.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.crm.mapper.CrmSupplierProjectMapper;
import com.ruoyi.crm.domain.CrmSupplierProject;
import com.ruoyi.crm.service.ICrmSupplierProjectService;

/**
 * 供应商产品关联Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-29
 */
@Service
public class CrmSupplierProjectServiceImpl implements ICrmSupplierProjectService 
{
    @Autowired
    private CrmSupplierProjectMapper crmSupplierProjectMapper;

    /**
     * 查询供应商产品关联
     * 
     * @param id 供应商产品关联主键
     * @return 供应商产品关联
     */
    @Override
    public CrmSupplierProject selectCrmSupplierProjectById(Long id)
    {
        return crmSupplierProjectMapper.selectCrmSupplierProjectById(id);
    }

    /**
     * 查询供应商产品关联列表
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 供应商产品关联
     */
    @Override
    public List<CrmSupplierProject> selectCrmSupplierProjectList(CrmSupplierProject crmSupplierProject)
    {
        return crmSupplierProjectMapper.selectCrmSupplierProjectList(crmSupplierProject);
    }

    /**
     * 新增供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    @Override
    public int insertCrmSupplierProject(CrmSupplierProject crmSupplierProject)
    {
        crmSupplierProject.setCreateTime(DateUtils.getNowDate());
        return crmSupplierProjectMapper.insertCrmSupplierProject(crmSupplierProject);
    }

    /**
     * 修改供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    @Override
    public int updateCrmSupplierProject(CrmSupplierProject crmSupplierProject)
    {
        crmSupplierProject.setUpdateTime(DateUtils.getNowDate());
        return crmSupplierProjectMapper.updateCrmSupplierProject(crmSupplierProject);
    }

    /**
     * 批量删除供应商产品关联
     * 
     * @param ids 需要删除的供应商产品关联主键
     * @return 结果
     */
    @Override
    public int deleteCrmSupplierProjectByIds(Long[] ids)
    {
        return crmSupplierProjectMapper.deleteCrmSupplierProjectByIds(ids);
    }

    /**
     * 删除供应商产品关联信息
     * 
     * @param id 供应商产品关联主键
     * @return 结果
     */
    @Override
    public int deleteCrmSupplierProjectById(Long id)
    {
        return crmSupplierProjectMapper.deleteCrmSupplierProjectById(id);
    }
}
