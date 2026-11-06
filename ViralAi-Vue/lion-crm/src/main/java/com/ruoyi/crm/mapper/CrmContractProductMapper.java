package com.ruoyi.crm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmContractProduct;
import com.ruoyi.crm.dto.CrmContractProductDto;

/**
 * 合同模型产品明细Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractProductMapper
{
    /**
     * 查询明细（含模型名称回显）
     *
     * @param contractId 合同主键
     * @return 明细集合
     */
    public List<CrmContractProductDto> selectContractProductList(Long contractId);

    /**
     * 查询单条明细
     *
     * @param id 明细主键
     * @return 明细
     */
    public CrmContractProduct selectContractProductById(Long id);

    /**
     * 统计合同下有效明细数量
     *
     * @param contractId 合同主键
     * @return 数量
     */
    public int countContractProductByContractId(Long contractId);

    /**
     * 批量新增明细
     *
     * @param list 明细集合
     * @return 结果
     */
    public int batchInsertContractProduct(@Param("list") List<CrmContractProduct> list);

    /**
     * 按合同删除全部明细（全量替换时使用）
     *
     * @param contractId 合同主键
     * @return 结果
     */
    public int deleteContractProductByContractId(Long contractId);

    /**
     * 删除单条明细
     *
     * @param id 明细主键
     * @return 结果
     */
    public int deleteContractProductById(Long id);
}
