package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmContractBillDiff;
import com.ruoyi.crm.dto.CrmContractBillDiffDto;

/**
 * 对账差异Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractBillDiffMapper
{
    /**
     * 查询差异列表
     *
     * @param query 查询条件
     * @return 差异列表
     */
    public List<CrmContractBillDiffDto> selectCrmContractBillDiffList(CrmContractBillDiffDto query);

    /**
     * 查询差异详情
     *
     * @param id 主键
     * @return 差异
     */
    public CrmContractBillDiffDto selectCrmContractBillDiffById(Long id);

    /**
     * 查询差异实体
     *
     * @param id 主键
     * @return 差异
     */
    public CrmContractBillDiff selectCrmContractBillDiffEntity(Long id);

    /**
     * 新增差异
     *
     * @param diff 差异
     * @return 结果
     */
    public int insertCrmContractBillDiff(CrmContractBillDiff diff);

    /**
     * 更新差异处理结果
     *
     * @param id 主键
     * @param handleStatus 处理状态
     * @param handleRemark 处理意见
     * @param handler 处理人
     * @param handleTime 处理时间
     * @return 结果
     */
    public int updateDiffHandle(@Param("id") Long id,
                                @Param("handleStatus") Integer handleStatus,
                                @Param("handleRemark") String handleRemark,
                                @Param("handler") Long handler,
                                @Param("handleTime") Date handleTime);

    /**
     * 统计待处理差异数量
     *
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @param startTime 调用时间起始，可为空
     * @param endTime 调用时间结束，可为空
     * @return 条数
     */
    public int countPending(@Param("supplierIds") List<Long> supplierIds,
                            @Param("startTime") Date startTime,
                            @Param("endTime") Date endTime);
}
