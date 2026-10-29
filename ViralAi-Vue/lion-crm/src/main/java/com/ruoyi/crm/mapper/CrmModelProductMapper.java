package com.ruoyi.crm.mapper;

import java.util.List;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.dto.CrmModelProductDto;

/**
 * CRM模型产品Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
public interface CrmModelProductMapper 
{
    /**
     * 查询CRM模型产品
     * 
     * @param id CRM模型产品主键
     * @return CRM模型产品
     */
    public CrmModelProduct selectCrmModelProductById(Long id);

    /**
     * 查询CRM模型产品列表
     * 
     * @param crmModelProduct CRM模型产品
     * @return CRM模型产品集合
     */
    public List<CrmModelProductDto> selectCrmModelProductList(CrmModelProduct crmModelProduct);

    /**
     * 新增CRM模型产品
     * 
     * @param crmModelProduct CRM模型产品
     * @return 结果
     */
    public int insertCrmModelProduct(CrmModelProduct crmModelProduct);

    /**
     * 修改CRM模型产品
     * 
     * @param crmModelProduct CRM模型产品
     * @return 结果
     */
    public int updateCrmModelProduct(CrmModelProduct crmModelProduct);

    /**
     * 删除CRM模型产品
     * 
     * @param id CRM模型产品主键
     * @return 结果
     */
    public int deleteCrmModelProductById(Long id);

    /**
     * 批量删除CRM模型产品
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCrmModelProductByIds(Long[] ids);
}
