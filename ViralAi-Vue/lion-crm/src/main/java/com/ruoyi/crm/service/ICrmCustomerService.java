package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
import com.ruoyi.crm.dto.CrmCustomerVo;

/**
 * CRM 客户Service接口
 * <p>
 * 客户资料只维护客户自身属性：客户与渠道的绑定义务已下移到商机、合同明细行，
 * 由明细保存时的强制性校验保证「渠道 + 模型 + 渠道账户」三者一致。
 *
 * @author ruoyi
 * @date 2026-10-26
 */
public interface ICrmCustomerService
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
     * @param crmCustomerVo CRM 客户
     * @return 结果
     */
    public int insertCrmCustomer(CrmCustomerVo crmCustomerVo);

    /**
     * 修改CRM 客户
     *
     * @param crmCustomerVo CRM 客户
     * @return 结果
     */
    public int updateCrmCustomer(CrmCustomerVo crmCustomerVo);

    /**
     * 批量删除CRM 客户
     * 
     * @param ids 需要删除的CRM 客户主键集合
     * @return 结果
     */
    public int deleteCrmCustomerByIds(Long[] ids);

    /**
     * 删除CRM 客户信息
     * 
     * @param id CRM 客户主键
     * @return 结果
     */
    public int deleteCrmCustomerById(Long id);
}
