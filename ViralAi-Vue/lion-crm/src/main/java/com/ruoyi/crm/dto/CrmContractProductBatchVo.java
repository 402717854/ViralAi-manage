package com.ruoyi.crm.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同明细批量保存入参
 *
 * <p>list 为全量明细：为空数组表示清空该合同全部明细；为 null 时按空集合处理。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractProductBatchVo
{
    /** 合同ID */
    private Long contractId;

    /** 明细集合 */
    private List<CrmContractProductDto> list;
}
