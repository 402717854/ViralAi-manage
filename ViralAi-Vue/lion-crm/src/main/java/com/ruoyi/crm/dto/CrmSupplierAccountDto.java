package com.ruoyi.crm.dto;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 渠道账户查询结果对象
 * <p>
 * 在渠道账户主表字段之外，附加供应商名称与创建人/修改人名称，供列表与详情页直接展示。
 * 列表查询返回的 apiKey 已脱敏，完整密钥只在详情接口返回。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmSupplierAccountDto extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账户名 */
    @Excel(name = "账户名")
    private String accountName;

    /** 账户密码（列表中不返回） */
    private String accountPwd;

    /** apiKey 编号 */
    @Excel(name = "apiKey编号")
    private String apiKeyNo;

    /** apiKey（列表为脱敏值） */
    @Excel(name = "apiKey")
    private String apiKey;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    @Excel(name = "供应商")
    private String supplierName;

    /** 状态 0未启用 1启用 */
    @Excel(name = "状态")
    private Integer status;

    /** 账户类型 1测试账户 2真实账户 */
    @Excel(name = "账户类型")
    private Integer type;

    /** 流水拉取方式 1路由获取 2文件上传 */
    @Excel(name = "拉取方式")
    private Integer pullType;

    /** 流水拉取适配器 viralaiai/pixverse，空=通用 */
    @Excel(name = "拉取适配器")
    private String pullChannel;

    /** 流水拉取接口地址 */
    @Excel(name = "拉取地址")
    private String pullUrl;

    /** 流水拉取的用户名过滤 */
    @Excel(name = "拉取用户名")
    private String pullUser;

    /** 创建者ID */
    private Long creator;

    /** 创建者名称 */
    @Excel(name = "创建者")
    private String creatorName;

    /** 修改者ID */
    private Long updater;

    /** 修改者名称 */
    @Excel(name = "修改者")
    private String updaterName;
}
