package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同新建初始化信息
 *
 * <p>根据商机带出客户、渠道信息，此时不生成合同编号；渠道由商机锁定，前端只读展示。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractInitVo
{
    /** 商机ID */
    private Long busId;

    /** 商机名称 */
    private String busName;

    /** 客户ID */
    private Long consumerId;

    /** 客户名称 */
    private String consumerName;

    /** 建议合同名称，取「商机名称 + 合同」 */
    private String suggestedContractName;
}
