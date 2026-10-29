package com.ruoyi.crm.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmSupplierProject;
import com.ruoyi.crm.dto.CrmSupplierDto;
import com.ruoyi.crm.dto.CrmSupplierOptionDto;
import com.ruoyi.crm.dto.CrmSupplierProjectDto;
import com.ruoyi.crm.dto.CrmSupplierVo;
import com.ruoyi.crm.mapper.CrmSupplierProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.crm.mapper.CrmSupplierMapper;
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.service.ICrmSupplierService;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRM 供应商Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
@Service
public class CrmSupplierServiceImpl implements ICrmSupplierService 
{
    @Autowired
    private CrmSupplierMapper crmSupplierMapper;

    @Autowired
    private CrmSupplierProjectMapper crmSupplierProjectMapper;

    /**
     * 查询CRM 供应商详情（含产品关联列表）
     * 
     * @param id CRM 供应商主键
     * @return CRM 供应商详情，不存在时返回 null
     */
    @Override
    public CrmSupplierDto selectCrmSupplierById(Long id)
    {
        if (id == null)
        {
            return null;
        }
        CrmSupplier crmSupplier = crmSupplierMapper.selectCrmSupplierById(id);
        if (crmSupplier == null)
        {
            return null;
        }
        CrmSupplierDto crmSupplierDto = new CrmSupplierDto();
        BeanUtils.copyProperties(crmSupplier, crmSupplierDto);
        crmSupplierDto.setProjects(selectProjectsBySupplierId(id));
        return crmSupplierDto;
    }

    /**
     * 查询CRM 供应商列表
     * 
     * @param crmSupplier CRM 供应商
     * @return CRM 供应商
     */
    @Override
    public List<CrmSupplierDto> selectCrmSupplierList(CrmSupplier crmSupplier)
    {
        return crmSupplierMapper.selectCrmSupplierList(crmSupplier);
    }

    /**
     * 查询所有可用状态的CRM 供应商选项
     *
     * @return CRM 供应商选项集合（仅 id、name）
     */
    @Override
    public List<CrmSupplierOptionDto> selectAvailableSupplierOptions()
    {
        return crmSupplierMapper.selectAvailableSupplierOptions();
    }

    /**
     * 新增CRM 供应商，并同步保存产品关联列表
     * 
     * @param crmSupplierVo CRM 供应商（含 projects 列表）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCrmSupplier(CrmSupplierVo crmSupplierVo)
    {
        Date nowDate = DateUtils.getNowDate();
        Long userId = SecurityUtils.getLoginUser().getUserId();

        CrmSupplier crmSupplier = new CrmSupplier();
        BeanUtils.copyProperties(crmSupplierVo, crmSupplier);
        crmSupplier.setCreator(userId);
        crmSupplier.setUpdater(userId);
        crmSupplier.setCreateTime(nowDate);
        int rows = crmSupplierMapper.insertCrmSupplier(crmSupplier);

        // 主表插入后主键回填至 crmSupplier，用回填值绑定关联记录，避免使用前端传入的 id
        saveSupplierProjects(crmSupplier.getId(), crmSupplierVo.getProjects(), nowDate, userId);
        return rows;
    }

    /**
     * 修改CRM 供应商，并按需替换产品关联列表
     * 
     * @param crmSupplierVo CRM 供应商（含 projects 列表，null 表示不涉及关联变更）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCrmSupplier(CrmSupplierVo crmSupplierVo)
    {
        Date nowDate = DateUtils.getNowDate();
        Long userId = SecurityUtils.getLoginUser().getUserId();

        CrmSupplier crmSupplier = new CrmSupplier();
        BeanUtils.copyProperties(crmSupplierVo, crmSupplier);
        crmSupplier.setUpdater(userId);
        crmSupplier.setUpdateTime(nowDate);
        int rows = crmSupplierMapper.updateCrmSupplier(crmSupplier);

        saveSupplierProjects(crmSupplierVo.getId(), crmSupplierVo.getProjects(), nowDate, userId);
        return rows;
    }

    /**
     * 批量删除CRM 供应商
     * 
     * @param ids 需要删除的CRM 供应商主键
     * @return 结果
     */
    @Override
    public int deleteCrmSupplierByIds(Long[] ids)
    {
        return crmSupplierMapper.deleteCrmSupplierByIds(ids);
    }

    /**
     * 删除CRM 供应商信息
     * 
     * @param id CRM 供应商主键
     * @return 结果
     */
    @Override
    public int deleteCrmSupplierById(Long id)
    {
        return crmSupplierMapper.deleteCrmSupplierById(id);
    }

    /**
     * 按供应商编号查询其产品关联明细（含模型型号、模型厂商）
     *
     * @param supplierId 供应商编号
     * @return 供应商产品关联明细集合
     */
    @Override
    public List<CrmSupplierProjectDto> selectSupplierProjectListBySupplierId(Long supplierId)
    {
        if (supplierId == null)
        {
            return Collections.emptyList();
        }
        return crmSupplierProjectMapper.selectSupplierProjectListBySupplierId(supplierId);
    }

    /**
     * 查询指定供应商的全部产品关联
     *
     * @param supplierId 供应商编号
     * @return 产品关联集合
     */
    private List<CrmSupplierProject> selectProjectsBySupplierId(Long supplierId)
    {
        CrmSupplierProject query = new CrmSupplierProject();
        query.setSupplierId(supplierId);
        return crmSupplierProjectMapper.selectCrmSupplierProjectList(query);
    }

    /**
     * 保存供应商产品关联（全量替换语义）
     * <p>
     * projects 为 null 时视为本次未提交关联信息，不做任何变更；
     * 为空数组时清空该供应商全部关联；非空时先删除旧关联再按提交内容重建。
     * 列表内 projectId 重复时静默去重，仅保留第一条；projectId 为空的行直接跳过。
     *
     * @param supplierId 供应商编号
     * @param projects   产品关联列表
     * @param nowDate    当前时间
     * @param userId     当前登录用户编号
     */
    private void saveSupplierProjects(Long supplierId, List<CrmSupplierProject> projects, Date nowDate, Long userId)
    {
        if (supplierId == null || projects == null)
        {
            return;
        }
        crmSupplierProjectMapper.deleteCrmSupplierProjectBySupplierId(supplierId);

        Set<Long> projectIds = new LinkedHashSet<>();
        for (CrmSupplierProject project : projects)
        {
            if (project == null || project.getProjectId() == null)
            {
                continue;
            }
            if (!projectIds.add(project.getProjectId()))
            {
                continue;
            }
            CrmSupplierProject entity = new CrmSupplierProject();
            entity.setSupplierId(supplierId);
            entity.setProjectId(project.getProjectId());
            entity.setMinCostDiscount(project.getMinCostDiscount());
            entity.setMaxCostDiscount(project.getMaxCostDiscount());
            entity.setMinSellDiscount(project.getMinSellDiscount());
            entity.setMaxSellDiscount(project.getMaxSellDiscount());
            entity.setMinAgentDiscount(project.getMinAgentDiscount());
            entity.setMaxAgentDiscount(project.getMaxAgentDiscount());
            entity.setDailyUsageAmount(project.getDailyUsageAmount());
            entity.setCreator(userId);
            entity.setUpdater(userId);
            entity.setCreateTime(nowDate);
            entity.setUpdateTime(nowDate);
            crmSupplierProjectMapper.insertCrmSupplierProject(entity);
        }
    }
}
