package com.ruoyi.crm.service.impl;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.crm.dto.CrmModelProductDto;
import com.ruoyi.crm.dto.CrmModelProductFormDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.crm.mapper.CrmModelProductMapper;
import com.ruoyi.crm.domain.CrmModelProduct;
import com.ruoyi.crm.service.ICrmModelProductService;

/**
 * CRM模型产品Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@Service
public class CrmModelProductServiceImpl implements ICrmModelProductService 
{
    /** 模型类型：1文本 */
    private static final int MODEL_TYPE_TEXT = 1;

    /** 模型类型：5视频处理 */
    private static final int MODEL_TYPE_VIDEO = 5;

    @Autowired
    private CrmModelProductMapper crmModelProductMapper;

    /**
     * 查询CRM模型产品
     * 
     * @param id CRM模型产品主键
     * @return CRM模型产品
     */
    @Override
    public CrmModelProduct selectCrmModelProductById(Long id)
    {
        return crmModelProductMapper.selectCrmModelProductById(id);
    }

    /**
     * 查询CRM模型产品列表
     * 
     * @param crmModelProduct CRM模型产品
     * @return CRM模型产品
     */
    @Override
    public List<CrmModelProductDto> selectCrmModelProductList(CrmModelProduct crmModelProduct)
    {
        return crmModelProductMapper.selectCrmModelProductList(crmModelProduct);
    }

    /**
     * 新增CRM模型产品
     * 
     * @param crmModelProduct CRM模型产品
     * @return 结果
     */
    @Override
    public int insertCrmModelProduct(CrmModelProduct crmModelProduct)
    {
        // type 列为 NOT NULL 且无默认值，必须在入库前校验，避免落库时报字段无默认值错误
        checkType(crmModelProduct.getType(), true);
        crmModelProduct.setCreateTime(DateUtils.getNowDate());
        // 状态允许为空，缺省按有效落库，与表默认值保持一致
        if (crmModelProduct.getStatus() == null)
        {
            crmModelProduct.setStatus(1);
        }
        return crmModelProductMapper.insertCrmModelProduct(crmModelProduct);
    }

    /**
     * 修改CRM模型产品
     * 
     * @param crmModelProduct CRM模型产品
     * @return 结果
     */
    @Override
    public int updateCrmModelProduct(CrmModelProduct crmModelProduct)
    {
        // 修改时 type 可缺省（表示不变更），一旦传入则必须是合法枚举值
        checkType(crmModelProduct.getType(), false);
        crmModelProduct.setUpdateTime(DateUtils.getNowDate());
        return crmModelProductMapper.updateCrmModelProduct(crmModelProduct);
    }

    /**
     * 批量删除CRM模型产品
     * 
     * @param ids 需要删除的CRM模型产品主键
     * @return 结果
     */
    @Override
    public int deleteCrmModelProductByIds(Long[] ids)
    {
        return crmModelProductMapper.deleteCrmModelProductByIds(ids);
    }

    /**
     * 删除CRM模型产品信息
     * 
     * @param id CRM模型产品主键
     * @return 结果
     */
    @Override
    public int deleteCrmModelProductById(Long id)
    {
        return crmModelProductMapper.deleteCrmModelProductById(id);
    }

    /**
     * 查询模型产品详情（含计费配置）
     *
     * @param id 模型产品主键
     * @return 模型产品与计费配置
     */
    @Override
    public CrmModelProductFormDto selectCrmModelProductDetail(Long id)
    {
        CrmModelProduct product = crmModelProductMapper.selectCrmModelProductById(id);
        if (product == null)
        {
            return null;
        }
        CrmModelProductFormDto form = new CrmModelProductFormDto();
        BeanUtils.copyProperties(product, form);
        return form;
    }

    /**
     * 新增模型产品并联动新增模型计费
     *
     * @param form 模型产品与计费配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCrmModelProductWithBilling(CrmModelProductFormDto form)
    {
        int rows = insertCrmModelProduct(form);
        return rows;
    }

    /**
     * 修改模型产品并联动维护模型计费
     *
     * @param form 模型产品与计费配置
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCrmModelProductWithBilling(CrmModelProductFormDto form)
    {
        CrmModelProduct old = form.getId() == null ? null
                : crmModelProductMapper.selectCrmModelProductById(form.getId());
        int rows = updateCrmModelProduct(form);
        return rows;
    }

    /**
     * 校验模型类型取值
     * 
     * @param type 模型类型
     * @param required 是否必填
     */
    private void checkType(Integer type, boolean required)
    {
        if (type == null)
        {
            if (required)
            {
                throw new ServiceException("模型类型不能为空");
            }
            return;
        }
        if (type < MODEL_TYPE_TEXT || type > MODEL_TYPE_VIDEO)
        {
            throw new ServiceException("模型类型取值非法，允许范围 1-5");
        }
    }
}
