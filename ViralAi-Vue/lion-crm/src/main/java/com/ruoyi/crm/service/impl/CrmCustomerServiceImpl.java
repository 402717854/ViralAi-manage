package com.ruoyi.crm.service.impl;

import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
import com.ruoyi.crm.dto.CrmCustomerVo;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.service.ICrmCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * CRM 客户Service业务层处理
 * <p>
 * 客户资料只维护客户自身属性。客户与渠道、模型的绑定关系由商机与合同的明细行承载，
 * 明细保存时在对应 Service 中做强制性校验，因此这里不再处理客户渠道关联。
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
     * @param crmCustomerVo CRM 客户
     * @return 结果
     */
    @Override
    public int insertCrmCustomer(CrmCustomerVo crmCustomerVo)
    {
        Date nowDate = DateUtils.getNowDate();
        crmCustomerVo.setOwnerTime(nowDate);
        crmCustomerVo.setCreateTime(nowDate);
        return crmCustomerMapper.insertCrmCustomer(crmCustomerVo);
    }

    /**
     * 修改CRM 客户
     * 
     * @param crmCustomerVo CRM 客户
     * @return 结果
     */
    @Override
    public int updateCrmCustomer(CrmCustomerVo crmCustomerVo)
    {
        if (crmCustomerVo.getId() == null)
        {
            throw new ServiceException("客户ID不能为空");
        }
        crmCustomerVo.setUpdateTime(DateUtils.getNowDate());
        return crmCustomerMapper.updateCrmCustomer(crmCustomerVo);
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
