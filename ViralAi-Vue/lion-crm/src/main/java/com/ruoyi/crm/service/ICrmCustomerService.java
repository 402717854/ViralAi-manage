package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.dto.CrmCustomerDto;
import com.ruoyi.crm.dto.CrmCustomerOptionDto;
import com.ruoyi.crm.dto.CrmCustomerProductDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierDto;
import com.ruoyi.crm.dto.CrmCustomerVo;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;

/**
 * CRM 客户Service接口
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
     * 查询客户关联的渠道列表
     *
     * @param customerId 客户ID
     * @return 渠道列表（含名称与编码）
     */
    public List<CrmCustomerSupplierDto> selectSupplierListByCustomerId(Long customerId);

    /**
     * 查询客户关联的渠道ID集合
     *
     * @param customerId 客户ID
     * @return 渠道ID集合
     */
    public List<Long> selectSupplierIdsByCustomerId(Long customerId);

    /**
     * 查询客户可用的模型清单（客户关联渠道 × 渠道已上架模型）
     * <p>
     * 商机与合同明细的唯一可选数据源，每行带所属渠道，供前端按渠道分组选择。
     *
     * @param customerId 客户ID
     * @return 可用模型清单
     */
    public List<CrmCustomerProductDto> selectProductsByCustomerId(Long customerId);

    /**
     * 查询客户可用渠道下的账户选项（商机/合同明细选择渠道账户）
     * <p>
     * 只返回客户已开通渠道下的账户，不含 apiKey、密码等敏感字段。
     *
     * @param customerId 客户ID
     * @return 渠道账户选项集合（含所属渠道ID与名称）
     */
    public List<CrmSupplierAccountDto> selectAccountOptionsByCustomerId(Long customerId);

    /**
     * 新增CRM 客户，并同步保存关联渠道
     *
     * @param crmCustomerVo CRM 客户（含 supplierIds）
     * @return 结果
     */
    public int insertCrmCustomer(CrmCustomerVo crmCustomerVo);

    /**
     * 修改CRM 客户，并按需替换关联渠道
     *
     * @param crmCustomerVo CRM 客户（supplierIds 为 null 时不涉及渠道变更）
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
