package com.ruoyi.crm.service.impl;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.domain.CrmSupplierProject;
import com.ruoyi.crm.dto.CrmChannelProductDto;
import com.ruoyi.crm.dto.CrmProjectChannelDto;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.dto.CrmSupplierDto;
import com.ruoyi.crm.dto.CrmSupplierOptionDto;
import com.ruoyi.crm.dto.CrmSupplierProjectDto;
import com.ruoyi.crm.dto.CrmSupplierVo;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
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

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

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
     * 查询全部可售模型（启用渠道 × 渠道已上架模型）
     *
     * @return 渠道模型选项集合（含所属渠道）
     */
    @Override
    public List<CrmChannelProductDto> selectChannelProductOptions()
    {
        return crmSupplierMapper.selectChannelProductOptions();
    }

    /**
     * 查询渠道账户选项（全部启用渠道下的账户，不含敏感字段）
     *
     * @return 渠道账户选项集合（含所属渠道）
     */
    @Override
    public List<CrmSupplierAccountDto> selectChannelAccountOptions()
    {
        return crmSupplierAccountMapper.selectChannelAccountOptions();
    }

    /**
     * 按模型ID查询报过价的渠道（含渠道名称与折扣区间）
     *
     * @param projectId 模型编号
     * @return 报价渠道集合
     */
    @Override
    public List<CrmProjectChannelDto> selectChannelListByProjectId(Long projectId)
    {
        if (projectId == null)
        {
            return Collections.emptyList();
        }
        return crmSupplierProjectMapper.selectChannelListByProjectId(projectId);
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
     * 按供应商编号查询其渠道账户列表
     * <p>
     * 复用渠道账户列表查询（resultMap 中 apiKey 已脱敏、不返回密码），
     * 因此详情页展示的密钥为掩码值，完整密钥仍需在渠道账户管理中查看。
     *
     * @param supplierId 供应商编号
     * @return 渠道账户集合
     */
    @Override
    public List<CrmSupplierAccountDto> selectAccountListBySupplierId(Long supplierId)
    {
        if (supplierId == null)
        {
            return Collections.emptyList();
        }
        CrmSupplierAccount query = new CrmSupplierAccount();
        query.setSupplierId(supplierId);
        return crmSupplierAccountMapper.selectCrmSupplierAccountList(query);
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
     * 保存供应商产品关联（以当前供应商为维度的全量替换）
     * <p>
     * projects 为 null 时视为本次未提交关联信息，不做任何变更；
     * 为空数组时清空该供应商全部关联；非空时先删除该供应商旧关联再按提交内容重建
     * （删除范围始终限定 supplier_id = 当前供应商，不会影响其它供应商的数据）。
     * <p>
     * 每个明细行承载「模型所属供应商 + 模型型号」：projectSupplierId 表示该模型取自哪个
     * 供应商的模型池，可与当前供应商不同——因此同一供应商的报价单里可以同时挂
     * B 的 gpt-4o 与 C 的 gpt-4o，各自定义不同的成本/售价折扣。
     * 未填 projectSupplierId 时落当前供应商。同一「模型所属供应商 + 模型」重复提交时
     * 静默去重，projectId 为空的行直接跳过。
     *
     * @param supplierId 当前供应商编号（报价归属方）
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

        Set<String> handledKeys = new LinkedHashSet<>();
        Map<Long, Boolean> supplierExists = new HashMap<>();
        for (CrmSupplierProject project : projects)
        {
            if (project == null || project.getProjectId() == null)
            {
                continue;
            }
            // 模型所属供应商：未填时视为当前供应商自己的模型池
            Long projectSupplierId = project.getProjectSupplierId() != null
                    ? project.getProjectSupplierId() : supplierId;
            Boolean exists = supplierExists.computeIfAbsent(projectSupplierId, key -> {
                CrmSupplier supplier = crmSupplierMapper.selectCrmSupplierById(key);
                return supplier != null && !Integer.valueOf(1).equals(supplier.getDeleted());
            });
            if (!Boolean.TRUE.equals(exists))
            {
                throw new ServiceException("产品明细中模型所属供应商不存在或已删除：" + projectSupplierId);
            }
            if (!handledKeys.add(projectSupplierId + ":" + project.getProjectId()))
            {
                continue;
            }
            CrmSupplierProject entity = new CrmSupplierProject();
            entity.setSupplierId(supplierId);
            entity.setProjectSupplierId(projectSupplierId);
            entity.setProjectId(project.getProjectId());
            entity.setMinCostDiscount(project.getMinCostDiscount());
            entity.setMaxCostDiscount(project.getMaxCostDiscount());
            entity.setMinSellDiscount(project.getMinSellDiscount());
            entity.setMaxSellDiscount(project.getMaxSellDiscount());
            entity.setCreator(userId);
            entity.setUpdater(userId);
            entity.setCreateTime(nowDate);
            entity.setUpdateTime(nowDate);
            crmSupplierProjectMapper.insertCrmSupplierProject(entity);
        }
    }
}
