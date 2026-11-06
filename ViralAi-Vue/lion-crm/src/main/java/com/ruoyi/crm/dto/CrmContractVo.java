package com.ruoyi.crm.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

/**
 * 合同保存入参
 *
 * <p>id 为空表示新建（生成合同编号，状态强制草稿）；id 非空表示草稿更新。
 * products 为 null 时不涉及明细变更；非 null 时全量替换明细，并执行渠道归属强校验。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractVo
{
    /** 合同ID */
    private Long id;

    /** 商机ID */
    private Long busId;

    /** 客户ID */
    private Long consumerId;

    /** 合同名称 */
    private String contractName;

    /** 签约主体 */
    private String contractEntity;

    /** 合同到期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;

    /** 备注 */
    private String remark;

    /** 模型产品明细，null 表示不变更 */
    private List<CrmContractProductDto> products;
}
