package com.ruoyi.crm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.ruoyi.crm.domain.CrmGatewayPullMap;
import com.ruoyi.crm.dto.CrmGatewayPullMapDto;

/**
 * 网关流水上游标识映射Mapper接口
 *
 * @author ruoyi
 * @date 2026-11-05
 */
public interface CrmGatewayPullMapMapper
{
    /**
     * 查询有效的映射规则
     * <p>
     * 拉取时一次性把涉及的渠道/账户规则全部读出来在内存里匹配，避免每行流水查一次库。
     * supplierIds 为空表示不限渠道。
     *
     * @param supplierIds 渠道ID集合，可为空
     * @param accountIds 账户ID集合，可为空
     * @param mapType 映射类型 CUSTOMER / PRODUCT，为空表示不限
     * @return 映射规则
     */
    public List<CrmGatewayPullMap> selectEffectiveMaps(@Param("supplierIds") List<Long> supplierIds,
                                                       @Param("accountIds") List<Long> accountIds,
                                                       @Param("mapType") String mapType);

    /**
     * 查询映射详情
     *
     * @param id 主键
     * @return 映射
     */
    public CrmGatewayPullMap selectCrmGatewayPullMapById(Long id);

    /**
     * 查询映射列表（按渠道/账户/类型过滤，附带渠道、账户、客户、模型名称）
     *
     * @param query 查询条件
     * @return 映射集合
     */
    public List<CrmGatewayPullMapDto> selectCrmGatewayPullMapList(CrmGatewayPullMap query);

    /**
     * 新增映射
     *
     * @param map 映射
     * @return 结果
     */
    public int insertCrmGatewayPullMap(CrmGatewayPullMap map);

    /**
     * 修改映射
     *
     * @param map 映射
     * @return 结果
     */
    public int updateCrmGatewayPullMap(CrmGatewayPullMap map);

    /**
     * 删除映射（物理删除，映射本身无业务数据含义）
     *
     * @param id 主键
     * @return 结果
     */
    public int deleteCrmGatewayPullMapById(Long id);

    /**
     * 统计同一维度下重复的映射数量
     *
     * @param map 映射
     * @param excludeId 修改时排除自身，新增传 null
     * @return 重复数量
     */
    public int countDuplicate(@Param("map") CrmGatewayPullMap map, @Param("excludeId") Long excludeId);
}
