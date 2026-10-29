package com.ruoyi.crm.service;

import java.util.List;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.dto.CrmModelProductDto;
import com.ruoyi.crm.dto.CrmModelProductFormDto;

/**
 * CRM模型产品Service接口
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
public interface ICrmModelProductService 
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
     * 批量删除CRM模型产品
     * 
     * @param ids 需要删除的CRM模型产品主键集合
     * @return 结果
     */
    public int deleteCrmModelProductByIds(Long[] ids);

    /**
     * 删除CRM模型产品信息
     * 
     * @param id CRM模型产品主键
     * @return 结果
     */
    public int deleteCrmModelProductById(Long id);

    /**
     * 查询模型产品详情（含计费配置）
     * <p>
     * billing 为 null 表示该产品尚未配置计费，前端据此决定是新建还是编辑计费区块。
     *
     * @param id 模型产品主键
     * @return 模型产品与计费配置
     */
    public CrmModelProductFormDto selectCrmModelProductDetail(Long id);

    /**
     * 新增模型产品并联动新增模型计费
     * <p>
     * 产品与计费在同一事务内落库，任一步失败整体回滚。计费编码缺省取模型型号，
     * 传了 billing 但模型型号为空时直接报错，避免产生无法关联的计费记录。
     *
     * @param form 模型产品与计费配置
     * @return 结果
     */
    public int insertCrmModelProductWithBilling(CrmModelProductFormDto form);

    /**
     * 修改模型产品并联动维护模型计费
     * <p>
     * 计费按模型编码 upsert：已存在则更新（价格明细全量替换），不存在则新增。billing 为 null 时不动计费配置。
     *
     * @param form 模型产品与计费配置
     * @return 结果
     */
    public int updateCrmModelProductWithBilling(CrmModelProductFormDto form);
}
