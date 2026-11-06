package com.ruoyi.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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
     * 按模型编码查询模型产品
     * <p>
     * 与对账引擎口径一致：{@code product_code} 为空的历史数据回退模型型号 {@code no}。
     *
     * @param productCode 模型编码
     * @return 模型产品，不存在返回 null
     */
    public CrmModelProduct selectCrmModelProductByProductCode(@Param("productCode") String productCode);

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

    /**
     * 查询指定渠道下已关联且启用的模型产品
     * <p>
     * 数据源为 crm_supplier_project（渠道-模型关联表），仅返回该渠道上架（status=1）的模型。
     * 合同明细新增只能使用该结果，禁止查询全量模型。
     *
     * @param supplierId 渠道ID
     * @return 模型产品集合
     */
    public List<CrmModelProductDto> selectProductsBySupplier(Long supplierId);
}
