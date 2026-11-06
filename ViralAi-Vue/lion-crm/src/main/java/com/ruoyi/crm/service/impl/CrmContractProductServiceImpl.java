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
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.domain.CrmContractLog;
import com.ruoyi.crm.domain.CrmContractProduct;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.dto.CrmCustomerProductDto;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.enums.ContractActionEnum;
import com.ruoyi.crm.enums.ContractStatusEnum;
import com.ruoyi.crm.mapper.CrmContractLogMapper;
import com.ruoyi.crm.mapper.CrmContractMapper;
import com.ruoyi.crm.mapper.CrmContractProductMapper;
import com.ruoyi.crm.mapper.CrmCustomerSupplierMapper;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.service.ICrmContractProductService;
import com.ruoyi.crm.service.support.CrmContractDataScope;

/**
 * 合同模型产品明细Service业务层处理
 *
 * <p>核心校验（前端绕过也无法写入脏数据）：</p>
 * <ol>
 *   <li>合同必须存在且为草稿状态；</li>
 *   <li>每行的「渠道 + 模型」必须命中该客户的已开通渠道及其上架模型（crm_customer_supplier × crm_supplier_project）；</li>
 *   <li>每行必须指定渠道账户（crm_supplier_account.id），且账户须属于该行所选渠道；</li>
 *   <li>同一合同内「渠道 + 模型」不可重复（同一模型可在不同渠道各出现一次）；</li>
 *   <li>成本折扣、售卖折扣取值范围 0.01 ~ 1.00，保留两位小数；</li>
 *   <li>价格明细必须为合法 JSON，长度不超过 2000。</li>
 * </ol>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmContractProductServiceImpl implements ICrmContractProductService
{
    /** 折扣下限 */
    private static final BigDecimal MIN_DISCOUNT = new BigDecimal("0.01");

    /** 折扣上限 */
    private static final BigDecimal MAX_DISCOUNT = BigDecimal.ONE;

    /** 价格明细最大长度，与 crm_contract_product.price_detail 字段长度一致 */
    private static final int PRICE_DETAIL_MAX_LENGTH = 2000;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private CrmContractMapper crmContractMapper;

    @Autowired
    private CrmContractProductMapper crmContractProductMapper;

    @Autowired
    private CrmCustomerSupplierMapper crmCustomerSupplierMapper;

    @Autowired
    private CrmSupplierAccountMapper crmSupplierAccountMapper;

    @Autowired
    private CrmContractLogMapper crmContractLogMapper;

    @Autowired
    private CrmContractDataScope crmContractDataScope;

    @Override
    public List<CrmContractProductDto> selectContractProductList(Long contractId)
    {
        if (contractId == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(contractId);
        crmContractDataScope.assertViewable(contract);
        return crmContractProductMapper.selectContractProductList(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveContractProduct(Long contractId, List<CrmContractProductDto> list)
    {
        if (contractId == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(contractId);
        if (contract == null)
        {
            throw new ServiceException("合同不存在或已删除");
        }
        crmContractDataScope.assertViewable(contract);

        ContractStatusEnum status = ContractStatusEnum.of(contract.getStatus());
        if (status == null)
        {
            throw new ServiceException("合同状态数据异常，无法维护模型明细");
        }
        if (!status.isEditable())
        {
            throw new ServiceException(StringUtils.format("合同当前为【{}】状态，仅草稿状态允许维护模型明细",
                    status.getInfo()));
        }

        Long consumerId = contract.getConsumerId();
        if (consumerId == null)
        {
            throw new ServiceException("合同未关联客户，无法维护模型明细");
        }

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        List<CrmContractProduct> entities = buildEntities(contractId, consumerId, list, userId, now);

        // 全量替换：先清空再写入，保证明细与提交内容一致
        crmContractProductMapper.deleteContractProductByContractId(contractId);
        int rows = 0;
        if (!entities.isEmpty())
        {
            rows = crmContractProductMapper.batchInsertContractProduct(entities);
        }

        writeLog(contractId, contract.getStatus(), contract.getStatus(),
                StringUtils.format("维护模型明细，共 {} 条", entities.size()));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteContractProductById(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("明细ID不能为空");
        }
        CrmContractProduct product = crmContractProductMapper.selectContractProductById(id);
        if (product == null)
        {
            throw new ServiceException("模型明细不存在或已删除");
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(product.getContractId());
        if (contract == null)
        {
            throw new ServiceException("合同不存在或已删除");
        }
        crmContractDataScope.assertViewable(contract);

        ContractStatusEnum status = ContractStatusEnum.of(contract.getStatus());
        if (status == null || !status.isEditable())
        {
            throw new ServiceException("仅草稿状态的合同允许删除模型明细");
        }

        int rows = crmContractProductMapper.deleteContractProductById(id);
        writeLog(contract.getId(), contract.getStatus(), contract.getStatus(),
                StringUtils.format("删除模型明细，产品ID {}", product.getProductId()));
        return rows;
    }

    /**
     * 构建并校验明细实体
     * <p>
     * 渠道由明细行承载：每行必须显式指定 supplierId，且「渠道 + 模型」必须命中
     * 客户已开通渠道及该渠道已上架的模型；同时每行必须指定该渠道下的账户（supplierAccountId）。
     *
     * @param contractId 合同ID
     * @param consumerId 客户ID
     * @param list 入参明细
     * @param userId 操作人
     * @param now 当前时间
     * @return 明细实体集合
     */
    private List<CrmContractProduct> buildEntities(Long contractId, Long consumerId,
                                                   List<CrmContractProductDto> list,
                                                   Long userId, Date now)
    {
        List<CrmContractProduct> entities = new ArrayList<>();
        if (list == null || list.isEmpty())
        {
            return entities;
        }

        // 客户可用模型：客户关联渠道 × 渠道已上架模型，键为「渠道ID:模型ID」
        Set<String> availableKeys = new HashSet<>();
        for (CrmCustomerProductDto product : crmCustomerSupplierMapper.selectProductsByCustomerId(consumerId))
        {
            if (product.getId() != null && product.getSupplierId() != null)
            {
                availableKeys.add(product.getSupplierId() + ":" + product.getId());
            }
        }
        if (availableKeys.isEmpty())
        {
            throw new ServiceException("该客户未开通任何渠道，或已开通渠道下没有可用模型，无法添加明细");
        }

        // 客户可用渠道账户：账户必须存在且属于该行选定的渠道
        Map<Long, CrmSupplierAccountDto> accountMap = new HashMap<>();
        for (CrmSupplierAccountDto account : crmSupplierAccountMapper.selectAccountOptionsByCustomerId(consumerId))
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
            CrmContractProductDto dto = list.get(i);
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
            String key = dto.getSupplierId() + ":" + dto.getProductId();
            if (!availableKeys.contains(key))
            {
                throw new ServiceException(StringUtils.format(
                        "第 {} 行模型不在该客户的可用渠道内（渠道需已开通且模型需已上架），禁止添加", rowNo));
            }
            if (!seenKeys.add(key))
            {
                throw new ServiceException(StringUtils.format("第 {} 行模型重复，同一渠道同一模型只能添加一次", rowNo));
            }

            CrmContractProduct entity = new CrmContractProduct();
            entity.setContractId(contractId);
            entity.setSupplierId(dto.getSupplierId());
            entity.setSupplierAccountId(dto.getSupplierAccountId());
            entity.setProductId(dto.getProductId());
            entity.setCostDiscount(normalizeDiscount(dto.getCostDiscount(), "成本折扣", rowNo));
            entity.setSellDiscount(normalizeDiscount(dto.getSellDiscount(), "售卖折扣", rowNo));
            entity.setPriceDetail(validatePriceDetail(dto.getPriceDetail(), rowNo));
            entity.setRemark(StringUtils.substring(dto.getRemark(), 0, 500));
            entity.setCreator(userId);
            entity.setCreateTime(now);
            entity.setUpdater(userId);
            entity.setUpdateTime(now);
            entity.setDeleted(0);
            entities.add(entity);
        }
        return entities;
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
     * 写入操作日志
     *
     * @param contractId 合同ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param content 操作内容
     */
    private void writeLog(Long contractId, Integer fromStatus, Integer toStatus, String content)
    {
        CrmContractLog log = new CrmContractLog();
        log.setContractId(contractId);
        log.setAction(ContractActionEnum.PRODUCT_SAVE.getCode());
        log.setActionName(ContractActionEnum.PRODUCT_SAVE.getName());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setContent(StringUtils.substring(content, 0, 1000));
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setCreateTime(DateUtils.getNowDate());
        crmContractLogMapper.insertCrmContractLog(log);
    }
}
