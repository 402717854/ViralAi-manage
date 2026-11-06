package com.ruoyi.crm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmBusinessProduct;
import com.ruoyi.crm.dto.CrmBusinessProductDto;

/**
 * 商机意向产品Mapper接口
 *
 * <p>表结构为 (bus_id, supplier_id, product_id) 联合主键，明细采用「全量替换」维护：
 * 保存时先按商机清空、再批量写入，因此不单独提供 update 方法。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmBusinessProductMapper
{
    /**
     * 查询商机产品清单（关联模型产品回显名称、型号、类型）
     *
     * @param busId 商机主键
     * @return 产品清单
     */
    public List<CrmBusinessProductDto> selectBusinessProductList(Long busId);

    /**
     * 统计商机产品条数
     *
     * @param busId 商机主键
     * @return 条数
     */
    public int countBusinessProductByBusId(Long busId);

    /**
     * 批量新增商机产品
     *
     * @param list 产品清单
     * @return 结果
     */
    public int batchInsertBusinessProduct(@Param("list") List<CrmBusinessProduct> list);

    /**
     * 按商机清空产品清单（全量替换时使用）
     *
     * @param busId 商机主键
     * @return 结果
     */
    public int deleteBusinessProductByBusId(Long busId);

    /**
     * 删除单条商机产品（按商机 + 渠道 + 模型定位，同一模型可存在于多个渠道）
     *
     * @param busId 商机主键
     * @param supplierId 渠道ID
     * @param productId 模型产品ID
     * @return 结果
     */
    public int deleteBusinessProduct(@Param("busId") Long busId, @Param("supplierId") Long supplierId,
                                     @Param("productId") Long productId);
}
