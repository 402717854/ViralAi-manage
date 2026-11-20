package com.ruoyi.crm.mapper;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.domain.CrmContractUsage;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.dto.CrmBillDailyGroupDto;
import com.ruoyi.crm.dto.CrmBillGroupDto;
import com.ruoyi.crm.dto.CrmBillUsageItemDto;
import com.ruoyi.crm.dto.CrmContractUsageDto;
import com.ruoyi.crm.dto.CrmCustomerSupplierStatVo;
import com.ruoyi.crm.dto.CrmMatchProductDto;
import com.ruoyi.crm.dto.CrmSupplierCostRankVo;
import com.ruoyi.crm.dto.CrmUsageBoardVo;
import com.ruoyi.crm.dto.CrmUsageTrendVo;

/**
 * 合同用量对账台账Mapper接口
 *
 * <p>除台账本身外，还承载对账所需的只读查询（合同、合同明细、模型基准价），
 * 目的是把「匹配」相关 SQL 收敛到一处，避免对既有合同的 Mapper 反复改动。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface CrmContractUsageMapper
{
    /**
     * 查询台账列表
     *
     * @param query 查询条件
     * @return 台账列表
     */
    public List<CrmContractUsageDto> selectCrmContractUsageList(CrmContractUsageDto query);

    /**
     * 查询台账详情
     *
     * @param id 主键
     * @return 台账
     */
    public CrmContractUsageDto selectCrmContractUsageById(Long id);

    /**
     * 查询台账实体（重新匹配、调账时需要原始字段）
     *
     * @param id 主键
     * @return 台账实体
     */
    public CrmContractUsage selectCrmContractUsageEntity(Long id);

    /**
     * 新增台账
     *
     * @param usage 台账
     * @return 结果
     */
    public int insertCrmContractUsage(CrmContractUsage usage);

    /**
     * 更新台账匹配结果
     *
     * @param usage 台账（仅匹配字段）
     * @return 结果
     */
    public int updateCrmContractUsage(CrmContractUsage usage);

    /**
     * 查询待（重新）匹配的台账
     *
     * @param startTime 调用时间起始
     * @param endTime 调用时间结束
     * @param supplierId 渠道ID，可为空
     * @param customerId 客户ID，可为空
     * @param matchStatuses 需要重试的匹配状态集合
     * @param scopeSupplierIds 数据权限渠道集合，可为空
     * @return 台账列表
     */
    public List<CrmContractUsage> selectRematchUsages(@Param("startTime") Date startTime,
                                                      @Param("endTime") Date endTime,
                                                      @Param("supplierId") Long supplierId,
                                                      @Param("customerId") Long customerId,
                                                      @Param("matchStatuses") List<Integer> matchStatuses,
                                                      @Param("scopeSupplierIds") List<Long> scopeSupplierIds);

    /**
     * 查询调用时间落在区间内的已签约合同（合同有效期过滤在 SQL 内完成）
     *
     * @param customerId 客户ID
     * @param supplierId 渠道ID
     * @param callTime 调用时间
     * @return 生效合同列表，正常应为 1 条
     */
    public List<CrmContract> selectMatchContracts(@Param("customerId") Long customerId,
                                                  @Param("supplierId") Long supplierId,
                                                  @Param("callTime") Date callTime);

    /**
     * 查询客户+渠道下的全部合同（用于诊断：为什么没匹配上）
     *
     * @param customerId 客户ID
     * @param supplierId 渠道ID
     * @return 合同列表
     */
    public List<CrmContract> selectRelatedContracts(@Param("customerId") Long customerId,
                                                    @Param("supplierId") Long supplierId);

    /**
     * 查询覆盖指定模型的生效合同明细候选（唯一性判定粒度）
     *
     * <p>口径：同一客户 + 同一渠道 + 同一账户 + 同一模型，同一时间只允许一行明细。
     * 调用方据返回条数判断：0 条为无可用明细，1 条为唯一匹配，多于 1 条属脏数据。</p>
     *
     * @param customerId 客户ID
     * @param supplierId 渠道ID
     * @param supplierAccountId 渠道账户ID，为空或 0 表示不过滤账户
     * @param productId 模型ID
     * @return 候选明细列表
     */
    public List<CrmMatchProductDto> selectMatchContractProducts(@Param("customerId") Long customerId,
                                                                @Param("supplierId") Long supplierId,
                                                                @Param("supplierAccountId") Long supplierAccountId,
                                                                @Param("productId") Long productId);

    /**
     * 按模型编码查询模型产品（含基准价）
     *
     * @param productCode 模型编码
     * @return 模型产品列表（编码重复时可能多条）
     */
    public List<CrmModelProduct> selectModelProductsByCode(@Param("productCode") String productCode);

    /**
     * 按合同+渠道+账期分组汇总可计费用量
     *
     * @param supplierId 渠道ID
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @return 分组结果
     */
    public List<CrmBillGroupDto> selectBillGroups(@Param("supplierId") Long supplierId,
                                                  @Param("startTime") Date startTime,
                                                  @Param("endTime") Date endTime);

    /**
     * 账单生成后回填 usage.bill_id，锁定该账期该合同的用量
     *
     * @param billId 账单ID
     * @param contractId 合同ID
     * @param supplierId 渠道ID
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @return 影响条数
     */
    public int updateBillIdByContract(@Param("billId") Long billId,
                                      @Param("contractId") Long contractId,
                                      @Param("supplierId") Long supplierId,
                                      @Param("startTime") Date startTime,
                                      @Param("endTime") Date endTime);

    /**
     * 查询账单归属的用量明细（详情抽屉分页用）
     *
     * <p>只取页面展示字段且不 join 维表，调用方需配合 {@code startPage()} 分页；
     * 账单可能归集整月流水，禁止无分页地全量拉取。</p>
     *
     * @param billId 账单ID
     * @return 用量明细行
     */
    public List<CrmBillUsageItemDto> selectUsageItemByBillId(@Param("billId") Long billId);

    /**
     * 统计账单归属的用量明细条数
     *
     * @param billId 账单ID
     * @return 条数
     */
    public int countUsageByBillId(@Param("billId") Long billId);

    /**
     * 按账单归属日期（{@code date(call_time)}）聚合用量，用于生成按天子账单
     *
     * <p>统计对象是本张账单已归集的用量（{@code bill_id = ?}），而不是时间区间：
     * 时间区间会把账单生成瞬间之后新增的用量也算进来，导致父子账单金额不一致。</p>
     *
     * @param billId 父账单ID
     * @return 按天聚合结果，按日期升序
     */
    public List<CrmBillDailyGroupDto> selectDailyGroupsByBillId(@Param("billId") Long billId);

    /**
     * 客户 + 渠道维度统计（金额只统计匹配成功的用量，调用数含异常）
     *
     * <p>按 {@code customer_id + supplier_id} 组合聚合，用于定位「某客户在某渠道」的应收、成本与毛利。</p>
     *
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @return 客户渠道统计
     */
    public List<CrmCustomerSupplierStatVo> selectCustomerSupplierStat(@Param("startTime") Date startTime,
                                                                      @Param("endTime") Date endTime,
                                                                      @Param("supplierIds") List<Long> supplierIds);

    /**
     * 渠道成本排行
     *
     * <p>按渠道聚合成本并倒序取前 N 条，成本为 0 的渠道会被过滤，避免空行占位。</p>
     *
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @param limit 取前 N 条
     * @return 排行
     */
    public List<CrmSupplierCostRankVo> selectSupplierCostRank(@Param("startTime") Date startTime,
                                                             @Param("endTime") Date endTime,
                                                             @Param("supplierIds") List<Long> supplierIds,
                                                             @Param("limit") Integer limit);

    /**
     * 看板概览
     *
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @return 概览
     */
    public CrmUsageBoardVo selectBoardSummary(@Param("startTime") Date startTime,
                                              @Param("endTime") Date endTime,
                                              @Param("supplierIds") List<Long> supplierIds);

    /**
     * 用量趋势：按账期聚合（单条 SQL 覆盖多期，避免逐期调用概览）
     *
     * <p>只返回有数据的账期，缺月由 Service 层补零填充，保证横轴连续。</p>
     *
     * @param startTime 起始（首期账期的 1 日 00:00:00）
     * @param endTime 结束（末期账期的月末最后一秒）
     * @param supplierIds 数据权限渠道集合，为空表示不限
     * @return 按账期升序的趋势
     */
    public List<CrmUsageTrendVo> selectUsageTrend(@Param("startTime") Date startTime,
                                                  @Param("endTime") Date endTime,
                                                  @Param("supplierIds") List<Long> supplierIds);

    /**
     * 查询在用量的渠道ID集合（月账单生成时遍历）
     *
     * @param startTime 周期起始
     * @param endTime 周期结束
     * @return 渠道ID集合
     */
    public List<Long> selectSupplierIdsWithUsage(@Param("startTime") Date startTime,
                                                 @Param("endTime") Date endTime);
}
