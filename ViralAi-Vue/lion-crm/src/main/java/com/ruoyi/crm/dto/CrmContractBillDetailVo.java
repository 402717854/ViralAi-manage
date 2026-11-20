package com.ruoyi.crm.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 账单详情传输对象
 *
 * <p>返回账单主体 + 同渠道同账期的差异记录 + 用量条数，供前端「账单详情」抽屉
 * 的三个区块渲染。</p>
 *
 * <p>用量明细<b>不再随详情一起返回</b>：账单按「合同 + 渠道 + 账期」归集，单张账单
 * 可能挂整月全部网关流水，一次全量返回会让响应体积与前端渲染成本随行数线性膨胀。
 * 明细改由 {@code GET /crm/bill/{id}/usages} 分页拉取，本对象只给出条数用于展示。</p>
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

    /** 归属用量明细条数 */
    private Integer usageCount;

    /** 同渠道同账期的差异记录 */
    private List<CrmContractBillDiffDto> diffs;

    /** 待处理差异数量 */
    private Integer pendingDiffCount;
}
