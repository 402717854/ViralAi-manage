package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmContractProductDto;

/**
 * 合同模型产品明细Service接口
 *
 * <p>明细维护仅在合同草稿状态可用，且强制校验明细产品归属合同渠道。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmContractProductService
{
    /**
     * 查询合同明细列表（含模型名称回显）
     *
     * @param contractId 合同ID
     * @return 明细集合
     */
    public List<CrmContractProductDto> selectContractProductList(Long contractId);

    /**
     * 明细批量保存（全量替换）
     *
     * @param contractId 合同ID
     * @param list 明细集合
     * @return 保存条数
     */
    public int batchSaveContractProduct(Long contractId, List<CrmContractProductDto> list);

    /**
     * 删除单条明细
     *
     * @param id 明细主键
     * @return 结果
     */
    public int deleteContractProductById(Long id);
}
