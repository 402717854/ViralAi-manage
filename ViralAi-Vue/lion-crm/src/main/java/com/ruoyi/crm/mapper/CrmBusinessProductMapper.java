package com.ruoyi.crm.mapper;

import java.util.List;
import com.ruoyi.crm.domain.CrmBusinessProduct;

/**
 * 商机渠道模型产品关联Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
public interface CrmBusinessProductMapper 
{
    /**
     * 查询商机渠道模型产品关联
     * 
     * @return 商机渠道模型产品关联
     */
    public CrmBusinessProduct selectOne(Long busId,Long supplierId,Long productId);

    /**
     * 查询商机渠道模型产品关联列表
     * 
     * @param crmBusinessProduct 商机渠道模型产品关联
     * @return 商机渠道模型产品关联集合
     */
    public List<CrmBusinessProduct> selectCrmBusinessProductList(CrmBusinessProduct crmBusinessProduct);

    /**
     * 新增商机渠道模型产品关联
     * 
     * @param crmBusinessProduct 商机渠道模型产品关联
     * @return 结果
     */
    public int insertCrmBusinessProduct(CrmBusinessProduct crmBusinessProduct);


    void update(CrmBusinessProduct businessProduct);
}
