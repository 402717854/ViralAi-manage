package com.ruoyi.crm.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 业务编号生成器Mapper接口
 *
 * <p>按 biz_type + supplier_code + gen_date 维度自增，数据库唯一索引兜底并发。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface SysNoGeneratorMapper
{
    /**
     * 序号自增：不存在则初始化 1，存在则 +1
     *
     * @param bizType 业务类型
     * @param supplierCode 渠道编码
     * @param genDate 生成日期 yyyyMMdd
     * @return 结果
     */
    public int incrementSeq(@Param("bizType") String bizType,
                            @Param("supplierCode") String supplierCode,
                            @Param("genDate") String genDate);

    /**
     * 查询当前已分配序号
     *
     * @param bizType 业务类型
     * @param supplierCode 渠道编码
     * @param genDate 生成日期 yyyyMMdd
     * @return 当前序号，不存在返回 null
     */
    public Integer selectSeq(@Param("bizType") String bizType,
                             @Param("supplierCode") String supplierCode,
                             @Param("genDate") String genDate);
}
