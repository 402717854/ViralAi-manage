package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.domain.CrmBusinessProduct;
import com.ruoyi.crm.dto.CrmBusinessProductDto;
import com.ruoyi.crm.dto.CrmCustomerProductDto;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.enums.BusStatusEnum;
import com.ruoyi.crm.enums.BusinessActionEnum;
import com.ruoyi.crm.mapper.CrmBusinessMapper;
import com.ruoyi.crm.mapper.CrmBusinessProductMapper;
import com.ruoyi.crm.mapper.CrmCustomerSupplierMapper;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.service.ICrmBusinessProductService;
import com.ruoyi.crm.service.support.CrmBusinessDataScope;
import com.ruoyi.crm.service.support.CrmBusinessLogRecorder;

/**
 * 商机意向产品Service业务层处理
 *
 * <p>核心校验（前端绕过也无法写入脏数据）：</p>
 * <ol>
 *   <li>商机必须存在且处于【初始化】【跟进中】（状态 &gt;= 2 后产品清单锁定）；</li>
 *   <li>每行的「渠道 + 模型」必须命中该客户已开通渠道及其上架模型（crm_customer_supplier × crm_supplier_project）；</li>
 *   <li>每行必须指定渠道账户（crm_supplier_account.id），且账户须属于该行所选渠道；</li>
 *   <li>同一商机内「渠道 + 模型」不可重复（同一模型可在不同渠道各出现一次）；</li>
 *   <li>成本折扣、售卖折扣取值范围 0.01 ~ 1.00，统一保留两位小数；</li>
 *   <li>价格明细必须为合法 JSON，长度不超过 2000。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmBusinessProductServiceImpl implements ICrmBusinessProductService
{
    /** 折扣下限 */
    private static final BigDecimal MIN_DISCOUNT = new BigDecimal("0.01");

    /** 折扣上限 */
    private static final BigDecimal MAX_DISCOUNT = BigDecimal.ONE;

    /** 价格明细最大长度，与 crm_business_product.price_detail 字段长度一致 */
    private static final int PRICE_DETAIL_MAX_LENGTH = 2000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

    @Autowired
    private CrmBusinessProductMapper crmBusinessProductMapper;

    @Autowired
    private CrmCustomerSupplierMapper crmCustomerSupplierMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmBusinessDataScope crmBusinessDataScope;

    @Autowired
    private CrmBusinessLogRecorder crmBusinessLogRecorder;

    @Override
    public List<CrmBusinessProductDto> selectBusinessProductList(Long busId)
    {
        if (busId == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(busId);
        crmBusinessDataScope.assertViewable(business);
        return crmBusinessProductMapper.selectBusinessProductList(busId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveBusinessProduct(Long busId, List<CrmBusinessProductDto> list)
    {
        CrmBusiness business = requireProductEditableBusiness(busId);
        Long customerId = business.getCustomerId();
        if (customerId == null)
        {
            throw new ServiceException("商机未关联客户，无法维护意向产品");
        }

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        List<CrmBusinessProduct> entities = buildEntities(busId, customerId, list, userId, now);

        // 全量替换：先清空再写入，保证清单与提交内容一致
        crmBusinessProductMapper.deleteBusinessProductByBusId(busId);
        int rows = 0;
        if (!entities.isEmpty())
        {
            rows = crmBusinessProductMapper.batchInsertBusinessProduct(entities);
        }

        crmBusinessLogRecorder.record(busId, BusinessActionEnum.PRODUCT,
                toIntStatus(business.getBusStatus()), toIntStatus(business.getBusStatus()),
                StringUtils.format("维护意向产品清单，共 {} 条", entities.size()));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBusinessProduct(Long busId, Long supplierId, Long productId)
    {
        if (productId == null)
        {
            throw new ServiceException("模型产品ID不能为空");
        }
        if (supplierId == null)
        {
            throw new ServiceException("渠道ID不能为空");
        }
        CrmBusiness business = requireProductEditableBusiness(busId);
        int rows = crmBusinessProductMapper.deleteBusinessProduct(busId, supplierId, productId);
        if (rows == 0)
        {
            throw new ServiceException("该模型不在当前商机的意向清单中");
        }
        crmBusinessLogRecorder.record(busId, BusinessActionEnum.PRODUCT,
                toIntStatus(business.getBusStatus()), toIntStatus(business.getBusStatus()),
                StringUtils.format("删除意向产品，渠道 {}，产品ID {}", supplierId, productId));
        return rows;
    }

    /**
     * 校验商机存在、在数据权限内且产品清单处于可维护状态
     *
     * @param busId 商机ID
     * @return 商机
     */
    private CrmBusiness requireProductEditableBusiness(Long busId)
    {
        if (busId == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(busId);
        crmBusinessDataScope.assertViewable(business);

        BusStatusEnum status = BusStatusEnum.of(business.getBusStatus());
        if (status == null)
        {
            throw new ServiceException("商机状态数据异常，无法维护意向产品");
        }
        if (!status.isProductEditable())
        {
            throw new ServiceException(StringUtils.format("商机当前为【{}】状态，仅初始化/跟进中可维护意向产品",
                    status.getInfo()));
        }
        return business;
    }

    /**
     * 构建并校验产品实体
     * <p>
     * 渠道由明细行承载：每行必须显式指定 supplierId，且「渠道 + 模型」必须命中
     * 客户已开通渠道及该渠道已上架的模型（crm_customer_supplier × crm_supplier_project）；
     * 同时每行必须指定该渠道下的账户（supplierAccountId），用于用量归属与密钥管理。
     *
     * @param busId 商机ID
     * @param customerId 客户ID
     * @param list 入参清单
     * @param userId 操作人
     * @param now 当前时间
     * @return 实体集合
     */
    private List<CrmBusinessProduct> buildEntities(Long busId, Long customerId, List<CrmBusinessProductDto> list,
                                                   Long userId, Date now)
    {
        List<CrmBusinessProduct> entities = new ArrayList<>();
        if (list == null || list.isEmpty())
        {
            return entities;
        }

        // 客户可用模型：客户关联渠道 × 渠道已上架模型，键为「渠道ID:模型ID」
        Set<String> availableKeys = new HashSet<>();
        for (CrmCustomerProductDto product : crmCustomerSupplierMapper.selectProductsByCustomerId(customerId))
        {
            if (product.getId() != null && product.getSupplierId() != null)
            {
                availableKeys.add(productKey(product.getSupplierId(), product.getId()));
            }
        }
        if (availableKeys.isEmpty())
        {
            throw new ServiceException("该客户未开通任何渠道，或已开通渠道下没有可用模型，无法添加意向产品");
        }

        // 客户可用渠道账户：账户必须存在且属于该行选定的渠道
        Map<Long, CrmSupplierAccountDto> accountMap = new HashMap<>();
        for (CrmSupplierAccountDto account : crmSupplierAccountMapper.selectAccountOptionsByCustomerId(customerId))
        {
            if (account.getId() != null && account.getSupplierId() != null)
            {
                accountMap.put(account.getId(), account);
            }
        }
        if (accountMap.isEmpty())
        {
            throw new ServiceException("该客户的已开通渠道下没有可用账户，请先在「渠道账户管理」中维护账户");
        }

        Set<String> seenKeys = new HashSet<>();
        for (int i = 0; i < list.size(); i++)
        {
            CrmBusinessProductDto dto = list.get(i);
            int rowNo = i + 1;
            if (dto == null || dto.getProductId() == null)
            {
                throw new ServiceException(StringUtils.format("第 {} 行未选择模型产品", rowNo));
            }
            if (dto.getSupplierId() == null)
            {
                throw new ServiceException(StringUtils.format("第 {} 行未指定所属渠道", rowNo));
            }
            if (dto.getSupplierAccountId() == null)
            {
                throw new ServiceException(StringUtils.format("第 {} 行未选择渠道账户", rowNo));
            }
            CrmSupplierAccountDto account = accountMap.get(dto.getSupplierAccountId());
            if (account == null)
            {
                throw new ServiceException(StringUtils.format(
                        "第 {} 行渠道账户不存在或不属于该客户的已开通渠道", rowNo));
            }
            if (!account.getSupplierId().equals(dto.getSupplierId()))
            {
                throw new ServiceException(StringUtils.format("第 {} 行渠道账户与所选渠道不匹配", rowNo));
            }
            String key = productKey(dto.getSupplierId(), dto.getProductId());
            if (!availableKeys.contains(key))
            {
                throw new ServiceException(StringUtils.format(
                        "第 {} 行模型不在该客户的可用渠道内（渠道需已开通且模型需已上架），禁止添加", rowNo));
            }
            if (!seenKeys.add(key))
            {
                throw new ServiceException(StringUtils.format("第 {} 行模型重复，同一渠道同一模型只能添加一次", rowNo));
            }

            CrmBusinessProduct entity = new CrmBusinessProduct();
            entity.setBusId(busId);
            entity.setSupplierId(dto.getSupplierId());
            entity.setSupplierAccountId(dto.getSupplierAccountId());
            entity.setProductId(dto.getProductId());
            entity.setCostDiscount(normalizeDiscount(dto.getCostDiscount(), "成本折扣", rowNo));
            entity.setSellDiscount(normalizeDiscount(dto.getSellDiscount(), "售卖折扣", rowNo));
            entity.setPriceDetail(validatePriceDetail(dto.getPriceDetail(), rowNo));
            entity.setCreator(userId);
            entity.setCreateTime(now);
            entities.add(entity);
        }
        return entities;
    }

    /**
     * 可用模型键：渠道 + 模型
     *
     * @param supplierId 渠道ID
     * @param productId 模型ID
     * @return 键
     */
    private String productKey(Long supplierId, Long productId)
    {
        return supplierId + ":" + productId;
    }

    /**
     * 折扣校验并统一保留两位小数
     *
     * @param discount 折扣值
     * @param label 字段名称
     * @param rowNo 行号
     * @return 规范化后的折扣
     */
    private BigDecimal normalizeDiscount(BigDecimal discount, String label, int rowNo)
    {
        if (discount == null)
        {
            throw new ServiceException(StringUtils.format("第 {} 行{}不能为空", rowNo, label));
        }
        BigDecimal value = discount.setScale(2, RoundingMode.HALF_UP);
        if (value.compareTo(MIN_DISCOUNT) < 0 || value.compareTo(MAX_DISCOUNT) > 0)
        {
            throw new ServiceException(StringUtils.format("第 {} 行{}取值范围为 0.01 ~ 1.00，保留两位小数", rowNo, label));
        }
        return value;
    }

    /**
     * 价格明细 JSON 校验
     *
     * @param priceDetail 价格明细
     * @param rowNo 行号
     * @return 校验通过的原值（空串归一为 null）
     */
    private String validatePriceDetail(String priceDetail, int rowNo)
    {
        if (StringUtils.isEmpty(priceDetail))
        {
            return null;
        }
        if (priceDetail.length() > PRICE_DETAIL_MAX_LENGTH)
        {
            throw new ServiceException(StringUtils.format("第 {} 行价格明细超过 {} 字符", rowNo, PRICE_DETAIL_MAX_LENGTH));
        }
        try
        {
            OBJECT_MAPPER.readTree(priceDetail);
        }
        catch (Exception e)
        {
            throw new ServiceException(StringUtils.format("第 {} 行价格明细不是合法的 JSON 格式", rowNo));
        }
        return priceDetail;
    }

    /**
     * 状态码转 int，供日志冗余字段使用
     *
     * @param status 状态码
     * @return int 状态，空值返回 null
     */
    private Integer toIntStatus(Long status)
    {
        return status == null ? null : status.intValue();
    }
}
