package com.ruoyi.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;

/**
 * 渠道账户Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmSupplierAccountMapper
{
    /**
     * 查询渠道账户列表（apiKey 脱敏、不返回密码）
     *
     * @param crmSupplierAccount 查询条件
     * @return 渠道账户集合
     */
    public List<CrmSupplierAccountDto> selectCrmSupplierAccountList(CrmSupplierAccount crmSupplierAccount);

    /**
     * 查询渠道账户选项（全部启用渠道下的账户）
     * <p>
     * 供商机/合同明细选择「渠道账户」使用：不返回 apiKey 与密码等敏感字段。
     * 客户不再维护关联渠道，账户范围由所选渠道决定，服务端保存时强制校验账户归属。
     *
     * @return 账户选项集合（id、accountName、supplierId、supplierName、status、type）
     */
    public List<CrmSupplierAccountDto> selectChannelAccountOptions();

    /**
     * 查询渠道账户详情（apiKey 为完整值，供编辑回填）
     *
     * @param id 主键
     * @return 渠道账户
     */
    public CrmSupplierAccountDto selectCrmSupplierAccountById(Long id);

    /**
     * 查询拉取流水用的账户清单（含完整 apiKey）
     * <p>
     * 仅供服务端内部调用：拉取上游流水需要真实密钥，不能下发到前端。
     * 三个参数组合决定范围：只传 accountId 取该账户；只传 supplierId 取该渠道下启用账户；
     * 都不传取全部启用账户（定时任务用）。
     *
     * @param supplierId 渠道ID，可为空
     * @param accountId 账户ID，可为空
     * @return 账户清单
     */
    public List<CrmSupplierAccount> selectAccountsForPull(@Param("supplierId") Long supplierId,
                                                          @Param("accountId") Long accountId);

    /**
     * 统计同一供应商下账户名重复数量
     *
     * @param supplierId 供应商ID
     * @param accountName 账户名
     * @param excludeId 需要排除的主键（修改时为自身，新增时传 null）
     * @return 重复数量
     */
    public int countBySupplierAndName(@Param("supplierId") Long supplierId,
                                      @Param("accountName") String accountName,
                                      @Param("excludeId") Long excludeId);

    /**
     * 新增渠道账户
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    public int insertCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount);

    /**
     * 修改渠道账户
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    public int updateCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount);
}
