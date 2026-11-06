package com.ruoyi.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.dto.CrmCustomerProductDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierDto;

/**
 * 客户渠道关联Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmCustomerSupplierMapper
{
    /**
     * 查询客户关联的渠道列表（含渠道名称与渠道编码）
     *
     * @param customerId 客户ID
     * @return 渠道列表
     */
    public List<CrmCustomerSupplierDto> selectSupplierListByCustomerId(Long customerId);

    /**
     * 查询客户关联的渠道ID集合（用于明细归属校验）
     *
     * @param customerId 客户ID
     * @return 渠道ID集合
     */
    public List<Long> selectSupplierIdsByCustomerId(Long customerId);

    /**
     * 查询客户可用的模型清单（客户关联渠道 × 渠道已上架模型）
     * <p>
     * 每行带 supplierId / supplierName，供商机与合同的明细分渠道选择；
     * 同一模型可被多个渠道上架，因此按「渠道+模型」返回多行。
     *
     * @param customerId 客户ID
     * @return 可用模型清单
     */
    public List<CrmCustomerProductDto> selectProductsByCustomerId(Long customerId);

    /**
     * 删除客户的全部渠道关联
     *
     * @param customerId 客户ID
     * @return 结果
     */
    public int deleteByCustomerId(Long customerId);

    /**
     * 批量新增客户渠道关联
     *
     * @param customerId 客户ID
     * @param supplierIds 渠道ID集合
     * @param creator 创建者
     * @return 结果
     */
    public int batchInsert(@Param("customerId") Long customerId,
                           @Param("supplierIds") List<Long> supplierIds,
                           @Param("creator") Long creator);
}
