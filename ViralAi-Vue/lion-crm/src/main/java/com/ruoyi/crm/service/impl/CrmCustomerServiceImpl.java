package com.ruoyi.crm.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.DictUtils;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
     * 新增CRM 客户
     * 
     * @param crmCustomer CRM 客户
     * @return 结果
     */
    @Override
    public int insertCrmCustomer(CrmCustomer crmCustomer)
    {
        Date nowDate = DateUtils.getNowDate();
        crmCustomer.setOwnerTime(nowDate);
        crmCustomer.setCreateTime(nowDate);
        return crmCustomerMapper.insertCrmCustomer(crmCustomer);
    }

    /**
     * 修改CRM 客户
     * 
     * @param crmCustomer CRM 客户
     * @return 结果
     */
    @Override
    public int updateCrmCustomer(CrmCustomer crmCustomer)
    {
        crmCustomer.setUpdateTime(DateUtils.getNowDate());
        return crmCustomerMapper.updateCrmCustomer(crmCustomer);
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
