package com.ruoyi.crm.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 期初余额批量导入请求对象
 *
 * <p>上线切换专用：历史已结算账单不回溯补流水，改为按客户实际在账余额导入，
 * 导入后账户余额即为真实余额，账实从此对齐。</p>
 *
 * @author ruoyi
 * @date 2026-11-20
 */
@Getter
@Setter
public class CrmFundInitVo
{
    /** 导入明细 */
    private List<Item> items;

    /**
     * 导入明细行
     */
    @Getter
    @Setter
    public static class Item
    {
        /** 客户ID */
        private Long customerId;

        /** 期初余额，必须 >= 0 */
        private BigDecimal amount;

        /** 备注 */
        private String remark;
    }
}
