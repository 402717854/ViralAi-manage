package com.ruoyi.crm.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单详情传输对象
 *
 * <p>一次返回：账单主体 + 归属用量明细 + 同渠道同账期的差异记录，
 * 供前端「账单详情」抽屉的三个区块直接渲染。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmContractBillDetailVo
{
    /** 账单主体 */
    private CrmContractBillDto bill;

    /** 归属用量明细 */
    private List<CrmContractUsageDto> usages;

    /** 同渠道同账期的差异记录 */
    private List<CrmContractBillDiffDto> diffs;

    /** 待处理差异数量 */
    private Integer pendingDiffCount;
}
