package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.domain.CrmGatewayPullMap;
import com.ruoyi.crm.dto.CrmGatewayPullMapDto;

/**
 * 网关流水上游标识映射Service接口
 *
 * @author ruoyi
 * @date 2026-11-05
 */
public interface ICrmGatewayPullMapService
{
    /**
     * 查询映射列表（附带渠道、账户、客户、模型名称）
     *
     * @param query 查询条件
     * @return 映射集合
     */
    public List<CrmGatewayPullMapDto> selectCrmGatewayPullMapList(CrmGatewayPullMap query);

    /**
     * 查询映射详情
     *
     * @param id 主键
     * @return 映射
     */
    public CrmGatewayPullMap selectCrmGatewayPullMapById(Long id);

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
     * 批量删除映射
     *
     * @param ids 主键集合
     * @return 结果
     */
    public int deleteCrmGatewayPullMapByIds(Long[] ids);
}
