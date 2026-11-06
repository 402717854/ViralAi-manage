package com.ruoyi.crm.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
import com.ruoyi.crm.dto.CrmCustomerProductDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierDto;
import com.ruoyi.crm.dto.CrmCustomerVo;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.mapper.CrmCustomerSupplierMapper;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.mapper.CrmSupplierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.service.ICrmCustomerService;

/**
 * CRM 客户Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
@Service
public class CrmCustomerServiceImpl implements ICrmCustomerService 
{
    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private CrmCustomerSupplierMapper crmCustomerSupplierMapper;

    @Autowired
    private CrmSupplierMapper crmSupplierMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    /**
     * 查询CRM 客户
     * 
     * @param id CRM 客户主键
     * @return CRM 客户
     */
    @Override
    public CrmCustomer selectCrmCustomerById(Long id)
    {
        return crmCustomerMapper.selectCrmCustomerById(id);
    }

    /**
     * 查询CRM 客户列表
     * 
     * @param crmCustomer CRM 客户
     * @return CRM 客户
     */
    @Override
    public List<CrmCustomerDto> selectCrmCustomerList(CrmCustomer crmCustomer)
    {
        return crmCustomerMapper.selectCrmCustomerList(crmCustomer);
    }

    /**
     * 查询所有可用状态的客户选项
     *
     * @return 仅包含 id、name 的客户选项集合
     */
    @Override
    public List<CrmCustomerOptionDto> selectAvailableCustomerOptions()
    {
        return crmCustomerMapper.selectAvailableCustomerOptions();
    }

    /**
     * 查询客户关联的渠道列表
     *
     * @param customerId 客户ID
     * @return 渠道列表（含名称与编码）
     */
    @Override
    public List<CrmCustomerSupplierDto> selectSupplierListByCustomerId(Long customerId)
    {
        if (customerId == null)
        {
            return new ArrayList<>();
        }
        return crmCustomerSupplierMapper.selectSupplierListByCustomerId(customerId);
    }

    /**
     * 查询客户关联的渠道ID集合
     *
     * @param customerId 客户ID
     * @return 渠道ID集合
     */
    @Override
    public List<Long> selectSupplierIdsByCustomerId(Long customerId)
    {
        if (customerId == null)
        {
            return new ArrayList<>();
        }
        return crmCustomerSupplierMapper.selectSupplierIdsByCustomerId(customerId);
    }

    /**
     * 查询客户可用的模型清单（客户关联渠道 × 渠道已上架模型）
     *
     * @param customerId 客户ID
     * @return 可用模型清单
     */
    @Override
    public List<CrmCustomerProductDto> selectProductsByCustomerId(Long customerId)
    {
        if (customerId == null)
        {
            return new ArrayList<>();
        }
        return crmCustomerSupplierMapper.selectProductsByCustomerId(customerId);
    }

    /**
     * 查询客户可用渠道下的账户选项
     *
     * @param customerId 客户ID
     * @return 渠道账户选项集合
     */
    @Override
    public List<CrmSupplierAccountDto> selectAccountOptionsByCustomerId(Long customerId)
    {
        if (customerId == null)
        {
            return new ArrayList<>();
        }
        return crmSupplierAccountMapper.selectAccountOptionsByCustomerId(customerId);
    }

    /**
     * 新增CRM 客户，并同步保存关联渠道
     * 
     * @param crmCustomerVo CRM 客户（含 supplierIds）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCrmCustomer(CrmCustomerVo crmCustomerVo)
    {
        Date nowDate = DateUtils.getNowDate();
        crmCustomerVo.setOwnerTime(nowDate);
        crmCustomerVo.setCreateTime(nowDate);
        int rows = crmCustomerMapper.insertCrmCustomer(crmCustomerVo);
        saveCustomerSuppliers(crmCustomerVo.getId(), crmCustomerVo.getSupplierIds());
        return rows;
    }

    /**
     * 修改CRM 客户，并按需替换关联渠道
     * 
     * @param crmCustomerVo CRM 客户（supplierIds 为 null 时不涉及渠道变更）
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCrmCustomer(CrmCustomerVo crmCustomerVo)
    {
        if (crmCustomerVo.getId() == null)
        {
            throw new ServiceException("客户ID不能为空");
        }
        crmCustomerVo.setUpdateTime(DateUtils.getNowDate());
        int rows = crmCustomerMapper.updateCrmCustomer(crmCustomerVo);
        saveCustomerSuppliers(crmCustomerVo.getId(), crmCustomerVo.getSupplierIds());
        return rows;
    }

    /**
     * 保存客户渠道关联（全量替换语义）
     * <p>
     * supplierIds 为 null 时不改动既有关系；为空集合时清空；非空时先删后插。
     * 渠道必须存在且未删除，重复ID自动去重。
     *
     * @param customerId 客户ID
     * @param supplierIds 渠道ID集合
     */
    private void saveCustomerSuppliers(Long customerId, List<Long> supplierIds)
    {
        if (customerId == null || supplierIds == null)
        {
            return;
        }
        Set<Long> distinctIds = new LinkedHashSet<>();
        for (Long supplierId : supplierIds)
        {
            if (supplierId == null)
            {
                continue;
            }
            CrmSupplier supplier = crmSupplierMapper.selectCrmSupplierById(supplierId);
            if (supplier == null || Integer.valueOf(1).equals(supplier.getDeleted()))
            {
                throw new ServiceException("渠道不存在或已删除：" + supplierId);
            }
            distinctIds.add(supplierId);
        }

        crmCustomerSupplierMapper.deleteByCustomerId(customerId);
        if (distinctIds.isEmpty())
        {
            return;
        }
        crmCustomerSupplierMapper.batchInsert(customerId, new ArrayList<>(distinctIds), SecurityUtils.getUserId());
    }

    /**
     * 批量删除CRM 客户
     * 
     * @param ids 需要删除的CRM 客户主键
     * @return 结果
     */
    @Override
    public int deleteCrmCustomerByIds(Long[] ids)
    {
        return crmCustomerMapper.deleteCrmCustomerByIds(ids);
    }

    /**
     * 删除CRM 客户信息
     * 
     * @param id CRM 客户主键
     * @return 结果
     */
    @Override
    public int deleteCrmCustomerById(Long id)
    {
        return crmCustomerMapper.deleteCrmCustomerById(id);
    }
}
