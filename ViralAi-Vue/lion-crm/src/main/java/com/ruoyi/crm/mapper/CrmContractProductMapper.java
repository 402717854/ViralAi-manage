package com.ruoyi.crm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmContractProduct;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierAccountVo;

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
     * 按主键更新明细（增量保存使用，保留明细ID）
     *
     * @param entity 明细实体
     * @return 结果
     */
    public int updateContractProduct(CrmContractProduct entity);

    /**
     * 按主键批量删除明细（增量保存时清理本次未提交的行）
     *
     * @param contractId 合同主键（限定范围，避免误删其他合同明细）
     * @param ids 明细主键集合
     * @return 结果
     */
    public int deleteContractProductByIds(@Param("contractId") Long contractId, @Param("ids") List<Long> ids);

    /**
     * 删除单条明细
     *
     * @param id 明细主键
     * @return 结果
     */
    public int deleteContractProductById(Long id);

    /**
     * 查询客户合同内出现过的「渠道 + 渠道账户」组合（去重）
     *
     * <p>供用量上传等场景做三级联动：客户由合同主表绑定，渠道与账户由明细行承载。
     * 只取已签约、已到期、已归档的合同，草稿与审批中的合同尚未生效，其明细不应作为
     * 流水归属依据；作废合同同理排除。</p>
     *
     * @param customerId 客户ID
     * @return 组合集合，按渠道、账户排序
     */
    public List<CrmCustomerSupplierAccountVo> selectSupplierAccountsByCustomer(@Param("customerId") Long customerId);
}
