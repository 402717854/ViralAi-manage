package com.ruoyi.crm.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmGatewayPullMap;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.domain.CrmSupplierAccount;
import com.ruoyi.crm.dto.CrmGatewayPullMapDto;
import com.ruoyi.crm.mapper.CrmGatewayPullMapMapper;
import com.ruoyi.crm.mapper.CrmModelProductMapper;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.service.ICrmGatewayPullMapService;

/**
 * 网关流水上游标识映射Service实现
 *
 * <p>映射配错的后果是「拉到的流水全部未映射」或「模型全部不在合同内」，
 * 所以在保存时就把能校验的都校验掉：类型合法、客户/模型存在、组合不重复。</p>
 *
 * @author ruoyi
 * @date 2026-11-05
 */
@Service
public class CrmGatewayPullMapServiceImpl implements ICrmGatewayPullMapService
{
    @Autowired
    private CrmGatewayPullMapMapper crmGatewayPullMapMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmModelProductMapper crmModelProductMapper;

    @Override
    public List<CrmGatewayPullMapDto> selectCrmGatewayPullMapList(CrmGatewayPullMap query)
    {
        return crmGatewayPullMapMapper.selectCrmGatewayPullMapList(query);
    }

    @Override
    public CrmGatewayPullMap selectCrmGatewayPullMapById(Long id)
    {
        return id == null ? null : crmGatewayPullMapMapper.selectCrmGatewayPullMapById(id);
    }

    @Override
    public int insertCrmGatewayPullMap(CrmGatewayPullMap map)
    {
        if (map == null)
        {
            throw new ServiceException("请求参数不能为空");
        }
        normalizeAndValidate(map);
        if (map.getStatus() == null)
        {
            map.setStatus(1);
        }
        map.setCreator(SecurityUtils.getUserId());
        map.setCreateTime(DateUtils.getNowDate());
        return crmGatewayPullMapMapper.insertCrmGatewayPullMap(map);
    }

    @Override
    public int updateCrmGatewayPullMap(CrmGatewayPullMap map)
    {
        if (map == null || map.getId() == null)
        {
            throw new ServiceException("映射ID不能为空");
        }
        CrmGatewayPullMap exist = crmGatewayPullMapMapper.selectCrmGatewayPullMapById(map.getId());
        if (exist == null)
        {
            throw new ServiceException("映射不存在或已被删除");
        }
        // 部分字段更新时，用库里已有值补齐后再校验，避免半截数据绕过唯一性检查
        if (map.getSupplierId() == null)
        {
            map.setSupplierId(exist.getSupplierId());
        }
        if (map.getSupplierAccountId() == null)
        {
            map.setSupplierAccountId(exist.getSupplierAccountId());
        }
        if (map.getMapType() == null)
        {
            map.setMapType(exist.getMapType());
        }
        if (map.getUpstreamKey() == null)
        {
            map.setUpstreamKey(exist.getUpstreamKey());
        }
        if (map.getUpstreamVariant() == null)
        {
            map.setUpstreamVariant(exist.getUpstreamVariant());
        }
        normalizeAndValidate(map);
        return crmGatewayPullMapMapper.updateCrmGatewayPullMap(map);
    }

    @Override
    public int deleteCrmGatewayPullMapByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        int count = 0;
        for (Long id : ids)
        {
            if (id != null)
            {
                count += crmGatewayPullMapMapper.deleteCrmGatewayPullMapById(id);
            }
        }
        return count;
    }

    /**
     * 规范化并校验映射
     *
     * @param map 映射
     */
    private void normalizeAndValidate(CrmGatewayPullMap map)
    {
        if (map.getSupplierId() == null)
        {
            throw new ServiceException("请选择渠道");
        }
        if (map.getSupplierAccountId() == null)
        {
            // 0 表示该渠道下所有账户通用
            map.setSupplierAccountId(0L);
        }
        else if (map.getSupplierAccountId() > 0)
        {
            List<CrmSupplierAccount> accounts = crmSupplierAccountMapper
                    .selectAccountsForPull(null, map.getSupplierAccountId());
            if (accounts == null || accounts.isEmpty())
            {
                throw new ServiceException("所选渠道账户不存在");
            }
            if (!map.getSupplierId().equals(accounts.get(0).getSupplierId()))
            {
                throw new ServiceException("所选账户不属于该渠道");
            }
        }

        String mapType = StringUtils.isEmpty(map.getMapType()) ? null : map.getMapType().trim().toUpperCase();
        if (mapType == null)
        {
            throw new ServiceException("请选择映射类型");
        }
        map.setMapType(mapType);
        map.setUpstreamKey(map.getUpstreamKey() == null ? "" : map.getUpstreamKey().trim());
        map.setUpstreamVariant(map.getUpstreamVariant() == null ? "" : map.getUpstreamVariant().trim());

        if (CrmGatewayPullMap.TYPE_CUSTOMER.equals(mapType))
        {
            if (map.getTargetId() == null)
            {
                throw new ServiceException("客户映射必须选择目标客户");
            }
            // 客户映射没有变体概念，强制清空避免误配后永远匹配不上
            map.setUpstreamVariant("");
            map.setTargetCode(null);
        }
        else if (CrmGatewayPullMap.TYPE_PRODUCT.equals(mapType))
        {
            if (StringUtils.isEmpty(map.getTargetCode()))
            {
                throw new ServiceException("模型映射必须选择目标模型");
            }
            if (map.getUpstreamKey().isEmpty())
            {
                throw new ServiceException("模型映射的上游模型标识不能为空");
            }
            CrmModelProduct product = crmModelProductMapper
                    .selectCrmModelProductByProductCode(map.getTargetCode());
            if (product == null)
            {
                throw new ServiceException("模型编码不存在：" + map.getTargetCode());
            }
            map.setTargetId(null);
        }
        else
        {
            throw new ServiceException("映射类型只能是 CUSTOMER 或 PRODUCT");
        }

        int dup = crmGatewayPullMapMapper.countDuplicate(map, map.getId());
        if (dup > 0)
        {
            throw new ServiceException("同一渠道/账户/类型/上游标识下已存在映射，请直接修改原记录");
        }
    }
}
