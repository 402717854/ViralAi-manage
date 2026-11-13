package com.ruoyi.crm.domain;

import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 渠道账户对象 crm_supplier_account
 * <p>
 * 一个渠道（供应商）可维护多个账户，用于对接测试与真实环境的密钥管理。
 * 该表无逻辑删除字段，删除即物理删除，本期仅提供新增、修改、查询。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmSupplierAccount extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 账户名 */
    private String accountName;

    /** 账户密码 */
    private String accountPwd;

    /** apiKey 编号 */
    private String apiKeyNo;

    /** apiKey */
    private String apiKey;

    /** 供应商（渠道）ID */
    private Long supplierId;

    /** 状态 0未启用 1启用 */
    private Integer status;

    /** 账户类型 1测试账户 2真实账户 */
    private Integer type;

    /**
     * 流水拉取方式：1 路由获取（系统调用接口自动拉取）2 文件上传（人工上传流水文件）
     * <p>两种方式是互斥的：文件上传的账户不参与定时路由拉取，因此 pullChannel/pullUrl/pullUser
     * 仅在该值为 1 时有意义，切换为 2 时由服务端清空，避免残留配置造成误解。</p>
     */
    private Integer pullType;

    /**
     * 流水拉取适配器：viralaiai / pixverse，为空表示走通用 HTTP 拉取（crm.gateway.pull-url）
     * <p>一个账户对应一套密钥与一种上游协议，因此拉取方式挂在账户上而不是渠道上
     * （同一渠道可能同时存在测试账户与真实账户，两者上游地址不同）。</p>
     */
    private String pullChannel;

    /** 流水拉取接口地址，为空时取适配器默认地址（pixverse 有 .ai/.cn 两套域名，中转站可自建） */
    private String pullUrl;

    /** 流水拉取的用户名过滤，为空表示不按用户过滤、拉取全量（中转站按 username 过滤客户） */
    private String pullUser;

    /** 创建者ID */
    private Long creator;

    /** 修改者ID */
    private Long updater;
}
