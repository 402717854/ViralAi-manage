package com.ruoyi.crm.mapper;

import java.util.List;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;

/**
 * CRM 客户Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-26
 */
public interface CrmCustomerMapper 
{
    /**
     * 查询CRM 客户
     * 
     * @param id CRM 客户主键
     * @return CRM 客户
     */
    public CrmCustomer selectCrmCustomerById(Long id);

    /**
     * 查询CRM 客户列表
     * 
     * @param crmCustomer CRM 客户
     * @return CRM 客户集合
     */
    public List<CrmCustomerDto> selectCrmCustomerList(CrmCustomer crmCustomer);

    /**
     * 查询所有可用状态的客户选项
     *
     * @return 仅包含 id、name 的客户选项集合
     */
    public List<CrmCustomerOptionDto> selectAvailableCustomerOptions();

    /**
     * 新增CRM 客户
     * 
     * @param crmCustomer CRM 客户
     * @return 结果
     */
    public int insertCrmCustomer(CrmCustomer crmCustomer);

    /**
     * 修改CRM 客户
     * 
     * @param crmCustomer CRM 客户
     * @return 结果
     */
    public int updateCrmCustomer(CrmCustomer crmCustomer);

    /**
     * 删除CRM 客户
     * 
     * @param id CRM 客户主键
     * @return 结果
     */
    public int deleteCrmCustomerById(Long id);

    /**
     * 批量删除CRM 客户
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCrmCustomerByIds(Long[] ids);
}
