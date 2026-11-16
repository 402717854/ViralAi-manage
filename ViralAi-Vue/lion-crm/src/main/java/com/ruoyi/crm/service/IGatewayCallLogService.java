package com.ruoyi.crm.service;

import java.util.List;
import java.util.Map;

import com.ruoyi.crm.domain.GatewayCallLog;
import com.ruoyi.crm.dto.GatewayCallLogDto;
import com.ruoyi.crm.dto.GatewayCallLogUploadDto;

/**
 * 网关调用流水Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface IGatewayCallLogService
{
    /**
     * 查询流水列表
     *
     * @param query 查询条件（已应用数据权限）
     * @return 流水列表
     */
    public List<GatewayCallLogDto> selectGatewayCallLogList(GatewayCallLogDto query);

    /**
     * 批量入库流水（按 call_no 幂等）
     *
     * @param logs 流水列表
     * @return 统计结果：inserted 新增数、duplicated 重复跳过数、invalid 非法跳过数
     */
    public Map<String, Object> ingest(List<GatewayCallLog> logs);

    /**
     * 按渠道 + 渠道账户拉取指定区间的流水并入库
     *
     * <p>范围：只给 supplierAccountId 拉该账户；只给 supplierId 拉该渠道下所有启用账户；
     * 都不给拉全部启用账户。上游协议由账户的 pullChannel 决定。</p>
     *
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @param supplierId 渠道ID，可为空
     * @param supplierAccountId 渠道账户ID，可为空
     * @return 统计结果：在 {@link #ingest(List)} 的键之外，追加 pulled 拉取数、
     *         unmapped 客户未映射数、missingModel 缺模型编码数、accounts 每账户明细
     */
    public Map<String, Object> pullAndIngest(String startTime, String endTime, Long supplierId,
                                             Long supplierAccountId);

    /**
     * 拉取指定渠道下全部启用账户的流水
     *
     * @param startTime 起始时间
     * @param endTime 结束时间
     * @param supplierId 渠道ID，可为空
     * @return 统计结果，同 {@link #pullAndIngest(String, String, Long, Long)}
     */
    public default Map<String, Object> pullAndIngest(String startTime, String endTime, Long supplierId)
    {
        return pullAndIngest(startTime, endTime, supplierId, null);
    }

    /**
     * 手工上传的流水文件解析结果入库
     *
     * <p>用于「拉取方式 = 文件上传」的渠道账户：上游不提供接口或不允许接口拉取时，
     * 由财务/运维导出流水文件后人工上传。客户由上传界面指定（一个文件归属一个客户），
     * 模型编码按 {@code crm_gateway_pull_map} 解析，与路由拉取口径一致，
     * 因此两种方式可以混用而不会算错账。</p>
     *
     * @param rows 解析后的文件行
     * @param supplierId 流水所属渠道，必填
     * @param supplierAccountId 渠道账户，必填；客户 / 渠道 / 账户三者必须是该客户生效合同明细里的组合
     * @param customerId 流水归属客户，必填
     * @param fileName 上传文件名，写入流水原始计费信息便于追溯来源
     * @param operator 上传人，写入流水原始计费信息便于追溯来源
     * @return 统计结果：在 {@link #ingest(List)} 的键之外，追加 parsed 解析行数、
     *         missingModel 缺模型编码数、rowErrors 行级错误样例、
     *         startTime / endTime 已入库流水的调用时间区间
     */
    public Map<String, Object> uploadAndIngest(List<GatewayCallLogUploadDto> rows, Long supplierId,
                                               Long supplierAccountId, Long customerId, String fileName,
                                               String operator);
}
