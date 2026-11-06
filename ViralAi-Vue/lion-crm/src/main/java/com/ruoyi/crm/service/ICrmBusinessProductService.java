package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmBusinessProductDto;

/**
 * 商机意向产品Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmBusinessProductService
{
    /**
     * 查询商机产品清单（含模型名称回显）
     *
     * @param busId 商机主键
     * @return 产品清单
     */
    public List<CrmBusinessProductDto> selectBusinessProductList(Long busId);

    /**
     * 批量保存商机产品（全量替换）
     *
     * <p>强校验：商机状态必须为初始化/跟进中、模型必须归属商机渠道、同商机不可重复、
     * 折扣 0.01~1.00 且保留两位小数、价格明细为合法 JSON。</p>
     *
     * @param busId 商机主键
     * @param list 产品清单，空集合表示清空
     * @return 写入条数
     */
    public int batchSaveBusinessProduct(Long busId, List<CrmBusinessProductDto> list);

    /**
     * 删除单条商机产品（按商机 + 渠道 + 模型定位）
     *
     * @param busId 商机主键
     * @param supplierId 渠道ID
     * @param productId 模型产品ID
     * @return 结果
     */
    public int deleteBusinessProduct(Long busId, Long supplierId, Long productId);
}
