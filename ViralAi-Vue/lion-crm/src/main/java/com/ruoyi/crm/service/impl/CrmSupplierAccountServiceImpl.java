package com.ruoyi.crm.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmSupplier;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.mapper.CrmSupplierMapper;
import com.ruoyi.crm.service.ICrmSupplierAccountService;
import com.ruoyi.crm.service.support.pull.GatewayPullAdapterFactory;

/**
 * 渠道账户Service业务层处理
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmSupplierAccountServiceImpl implements ICrmSupplierAccountService
{
    /** 账户名最大长度，与表结构保持一致 */
    private static final int MAX_ACCOUNT_NAME = 100;

    /** apiKey 编号最大长度 */
    private static final int MAX_API_KEY_NO = 100;

    /** apiKey 最大长度 */
    private static final int MAX_API_KEY = 300;

    /** 账户密码最大长度 */
    private static final int MAX_ACCOUNT_PWD = 255;

    /** 拉取地址最大长度 */
    private static final int MAX_PULL_URL = 255;

    /** 拉取用户名最大长度 */
    private static final int MAX_PULL_USER = 100;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmSupplierMapper crmSupplierMapper;

    @Autowired
    private GatewayPullAdapterFactory gatewayPullAdapterFactory;

    /**
     * 查询渠道账户列表（apiKey 脱敏）
     *
     * @param crmSupplierAccount 查询条件
     * @return 渠道账户集合
     */
    @Override
    public List<CrmSupplierAccountDto> selectCrmSupplierAccountList(CrmSupplierAccount crmSupplierAccount)
    {
        return crmSupplierAccountMapper.selectCrmSupplierAccountList(crmSupplierAccount);
    }

    /**
     * 查询渠道账户详情（apiKey 为完整值）
     *
     * @param id 主键
     * @return 渠道账户，不存在时返回 null
     */
    @Override
    public CrmSupplierAccountDto selectCrmSupplierAccountById(Long id)
    {
        if (id == null)
        {
            return null;
        }
        return crmSupplierAccountMapper.selectCrmSupplierAccountById(id);
    }

    /**
     * 新增渠道账户
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    @Override
    public int insertCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount)
    {
        if (crmSupplierAccount == null)
        {
            throw new ServiceException("请求参数不能为空");
        }
        validateAccount(crmSupplierAccount, null);
        if (StringUtils.isEmpty(crmSupplierAccount.getApiKey()))
        {
            throw new ServiceException("apiKey 不能为空");
        }
        // 新增默认启用，避免前端未选择状态时落库为空导致列表展示异常
        if (crmSupplierAccount.getStatus() == null)
        {
            crmSupplierAccount.setStatus(1);
        }
        // 空串统一存 NULL，避免出现 '' 与 NULL 两种「未填写」语义
        if (StringUtils.isEmpty(crmSupplierAccount.getAccountPwd()))
        {
            crmSupplierAccount.setAccountPwd(null);
        }
        if (StringUtils.isEmpty(crmSupplierAccount.getApiKeyNo()))
        {
            crmSupplierAccount.setApiKeyNo(null);
        }
        crmSupplierAccount.setCreator(SecurityUtils.getUserId());
        crmSupplierAccount.setCreateTime(DateUtils.getNowDate());
        return crmSupplierAccountMapper.insertCrmSupplierAccount(crmSupplierAccount);
    }

    /**
     * 修改渠道账户（密码留空表示不变更）
     *
     * @param crmSupplierAccount 渠道账户
     * @return 结果
     */
    @Override
    public int updateCrmSupplierAccount(CrmSupplierAccount crmSupplierAccount)
    {
        if (crmSupplierAccount == null || crmSupplierAccount.getId() == null)
        {
            throw new ServiceException("账户ID不能为空");
        }
        CrmSupplierAccountDto exist = crmSupplierAccountMapper.selectCrmSupplierAccountById(crmSupplierAccount.getId());
        if (exist == null)
        {
            throw new ServiceException("渠道账户不存在或已被删除");
        }
        validateAccount(crmSupplierAccount, crmSupplierAccount.getId());
        // 密码留空表示保持原密码不变，由 XML 的判空条件保证
        crmSupplierAccount.setUpdater(SecurityUtils.getUserId());
        return crmSupplierAccountMapper.updateCrmSupplierAccount(crmSupplierAccount);
    }

    /**
     * 公共校验：账户名、渠道、类型、状态与账户名唯一性
     *
     * @param account 账户信息
     * @param excludeId 修改时排除自身，新增传 null
     */
    private void validateAccount(CrmSupplierAccount account, Long excludeId)
    {
        String accountName = StringUtils.trim(account.getAccountName());
        if (StringUtils.isEmpty(accountName))
        {
            throw new ServiceException("账户名不能为空");
        }
        if (accountName.length() > MAX_ACCOUNT_NAME)
        {
            throw new ServiceException("账户名长度不能超过 " + MAX_ACCOUNT_NAME + " 个字符");
        }
        account.setAccountName(accountName);

        if (account.getApiKeyNo() != null && account.getApiKeyNo().length() > MAX_API_KEY_NO)
        {
            throw new ServiceException("apiKey 编号长度不能超过 " + MAX_API_KEY_NO + " 个字符");
        }
        if (account.getApiKey() != null && account.getApiKey().length() > MAX_API_KEY)
        {
            throw new ServiceException("apiKey 长度不能超过 " + MAX_API_KEY + " 个字符");
        }
        if (account.getAccountPwd() != null && account.getAccountPwd().length() > MAX_ACCOUNT_PWD)
        {
            throw new ServiceException("账户密码长度不能超过 " + MAX_ACCOUNT_PWD + " 个字符");
        }

        if (account.getSupplierId() == null)
        {
            throw new ServiceException("请选择所属供应商");
        }
        CrmSupplier supplier = crmSupplierMapper.selectCrmSupplierById(account.getSupplierId());
        if (supplier == null || Integer.valueOf(1).equals(supplier.getDeleted()))
        {
            throw new ServiceException("所选供应商不存在或已删除");
        }

        if (account.getType() == null)
        {
            throw new ServiceException("账户类型不能为空");
        }
        if (!Integer.valueOf(1).equals(account.getType()) && !Integer.valueOf(2).equals(account.getType()))
        {
            throw new ServiceException("账户类型只能是 1（测试账户）或 2（真实账户）");
        }
        if (account.getStatus() != null
                && !Integer.valueOf(0).equals(account.getStatus())
                && !Integer.valueOf(1).equals(account.getStatus()))
        {
            throw new ServiceException("状态只能是 0（未启用）或 1（启用）");
        }

        // 同一渠道下账户名不允许重复，避免对接时选错密钥
        int dup = crmSupplierAccountMapper.countBySupplierAndName(account.getSupplierId(), accountName, excludeId);
        if (dup > 0)
        {
            throw new ServiceException("该供应商下已存在同名账户：" + accountName);
        }

        validatePullConfig(account);
    }

    /**
     * 校验并规范化流水拉取配置
     *
     * <p>空串统一存 NULL（与其它「未填写」字段口径一致）；配了协议就必须是已注册的适配器，
     * 否则等到定时任务跑起来才发现配错，表现为「一条也拉不到」。</p>
     *
     * @param account 账户
     */
    private void validatePullConfig(CrmSupplierAccount account)
    {
        account.setPullChannel(StringUtils.isEmpty(account.getPullChannel())
                ? null : account.getPullChannel().trim());
        account.setPullUrl(StringUtils.isEmpty(account.getPullUrl()) ? null : account.getPullUrl().trim());
        account.setPullUser(StringUtils.isEmpty(account.getPullUser()) ? null : account.getPullUser().trim());

        if (account.getPullChannel() != null && !gatewayPullAdapterFactory.supports(account.getPullChannel()))
        {
            throw new ServiceException("不支持的拉取适配器：" + account.getPullChannel()
                    + "，可选值：" + String.join(" / ", gatewayPullAdapterFactory.channels()));
        }
        if (account.getPullUrl() != null && account.getPullUrl().length() > MAX_PULL_URL)
        {
            throw new ServiceException("拉取地址长度不能超过 " + MAX_PULL_URL + " 个字符");
        }
        if (account.getPullUser() != null && account.getPullUser().length() > MAX_PULL_USER)
        {
            throw new ServiceException("拉取用户名长度不能超过 " + MAX_PULL_USER + " 个字符");
        }
    }
}
