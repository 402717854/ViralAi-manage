package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;

/**
 * 渠道账户Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmSupplierAccountService
{
    /**
     * 查询渠道账户列表（apiKey 脱敏）
     *
     * @param crmSupplierAccount 查询条件
     * @return 渠道账户集合
     */
    public List<CrmSupplierAccountDto> selectCrmSupplierAccountList(CrmSupplierAccount crmSupplierAccount);

    /**
     * 查询渠道账户详情（apiKey 为完整值）
     *
     * @param id 主键
     * @return 渠道账户，不存在时返回 null
     */
    public CrmSupplierAccountDto selectCrmSupplierAccountById(Long id);

    /**
     * 新增渠道账户
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    public int insertCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount);

    /**
     * 修改渠道账户（密码留空表示不变更）
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    public int updateCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount);
}
