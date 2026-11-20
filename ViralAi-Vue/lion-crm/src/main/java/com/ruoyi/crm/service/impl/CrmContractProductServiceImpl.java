package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.ruoyi.crm.dto.CrmChannelProductDto;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.dto.CrmSupplierAccountDto;
import com.ruoyi.crm.enums.ContractActionEnum;
import com.ruoyi.crm.enums.ContractStatusEnum;
import com.ruoyi.crm.mapper.CrmContractLogMapper;
import com.ruoyi.crm.mapper.CrmContractMapper;
import com.ruoyi.crm.mapper.CrmContractProductMapper;
import com.ruoyi.crm.mapper.CrmSupplierAccountMapper;
import com.ruoyi.crm.mapper.CrmSupplierMapper;
import com.ruoyi.crm.service.ICrmContractProductService;
import com.ruoyi.crm.service.support.CrmContractDataScope;

/**
 * 合同模型产品明细Service业务层处理
 *
 * <p>核心校验（前端绕过也无法写入脏数据）：</p>
 * <ol>
 *   <li>合同必须存在且为草稿状态；</li>
 *   <li>每行的「渠道 + 模型」必须命中启用渠道及其已上架模型（crm_supplier × crm_supplier_project）；</li>
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
    private CrmSupplierMapper crmSupplierMapper;

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
        boolean signed = status == ContractStatusEnum.SIGNED;
        if (!status.isProductEditable())
        {
            throw new ServiceException(StringUtils.format(
                    "合同当前为【{}】状态，仅草稿或已签约状态允许维护模型明细", status.getInfo()));
        }

        Long consumerId = contract.getConsumerId();
        if (consumerId == null)
        {
            throw new ServiceException("合同未关联客户，无法维护模型明细");
        }

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();
        List<CrmContractProductDto> existing = crmContractProductMapper.selectContractProductList(contractId);
        // 已签约合同的既有行可能引用已下线模型或已失效账户，仅对新增行做可用性强校验；
        // 草稿态保持原有强校验口径不变
        Map<String, CrmContractProductDto> retainedMap = new HashMap<>();
        if (signed)
        {
            for (CrmContractProductDto old : existing)
            {
                retainedMap.put(productKey(old.getSupplierId(), old.getProductId()), old);
            }
        }
        List<CrmContractProduct> entities = buildEntities(contractId, list, userId, now, retainedMap);
        if (signed && entities.isEmpty())
        {
            throw new ServiceException("已签约合同的模型明细不能为空，请至少保留一条");
        }

        // 增量对齐：按「渠道 + 模型」比对既有明细，保留明细主键，
        // 避免对账台账 crm_contract_usage.contract_product_id 因全量替换而悬空
        SaveStat stat = saveIncremental(contractId, existing, entities);

        String content = StringUtils.format("维护模型明细，共 {} 条（新增 {} 条，更新 {} 条，删除 {} 条）",
                entities.size(), stat.inserted, stat.updated, stat.removed);
        if (signed)
        {
            content = "已签约合同变更明细：" + content;
        }
        writeLog(contractId, contract.getStatus(), contract.getStatus(), content);
        return entities.size();
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
        if (status == null || !status.isProductEditable())
        {
            throw new ServiceException(StringUtils.format(
                    "合同当前为【{}】状态，仅草稿或已签约状态允许删除模型明细",
                    status == null ? "未知" : status.getInfo()));
        }
        // 已签约合同需保留至少一条明细，否则后续对账无可用折扣基准
        if (status == ContractStatusEnum.SIGNED
                && crmContractProductMapper.countContractProductByContractId(contract.getId()) <= 1)
        {
            throw new ServiceException("已签约合同的模型明细不能为空，请至少保留一条");
        }

        int rows = crmContractProductMapper.deleteContractProductById(id);
        writeLog(contract.getId(), contract.getStatus(), contract.getStatus(),
                StringUtils.format("删除模型明细，产品ID {}", product.getProductId()));
        return rows;
    }

    /**
     * 构建并校验明细实体
     * <p>
     * 渠道模型关系由本模块的明细行维护：每行必须显式指定 supplierId、productId 与
     * supplierAccountId，且「渠道 + 模型」必须命中启用渠道及其已上架模型
     * （crm_supplier × crm_supplier_project），账户必须属于该行所选渠道。
     *
     * @param contractId 合同ID
     * @param list 入参明细
     * @param userId 操作人
     * @param now 当前时间
     * @param retainedMap 既有明细索引（键为「渠道:模型」），命中时按已签约合同的宽松口径校验：
     *                    沿用原账户可豁免账户有效性，改账户仍须为该渠道下的可用账户
     * @return 明细实体集合
     */
    private List<CrmContractProduct> buildEntities(Long contractId,
                                                   List<CrmContractProductDto> list,
                                                   Long userId, Date now,
                                                   Map<String, CrmContractProductDto> retainedMap)
    {
        List<CrmContractProduct> entities = new ArrayList<>();
        if (list == null || list.isEmpty())
        {
            return entities;
        }

        // 可售模型：启用渠道 × 渠道已上架模型，键为「渠道ID:模型ID」
        Set<String> availableKeys = new HashSet<>();
        for (CrmChannelProductDto product : crmSupplierMapper.selectChannelProductOptions())
        {
            if (product.getId() != null && product.getSupplierId() != null)
            {
                availableKeys.add(product.getSupplierId() + ":" + product.getId());
            }
        }
        if (availableKeys.isEmpty())
        {
            throw new ServiceException("系统内暂无可用渠道模型（渠道需启用且已上架有效模型），无法添加明细");
        }

        // 渠道账户：账户必须存在且属于该行选定的渠道
        Map<Long, CrmSupplierAccountDto> accountMap = new HashMap<>();
        for (CrmSupplierAccountDto account : crmSupplierAccountMapper.selectChannelAccountOptions())
        {
            if (account.getId() != null && account.getSupplierId() != null)
            {
                accountMap.put(account.getId(), account);
            }
        }
        if (accountMap.isEmpty())
        {
            throw new ServiceException("系统内暂无可用渠道账户，请先在「渠道账户管理」中维护账户");
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
            String key = dto.getSupplierId() + ":" + dto.getProductId();
            CrmContractProductDto old = retainedMap == null ? null : retainedMap.get(key);
            CrmSupplierAccountDto account = accountMap.get(dto.getSupplierAccountId());
            if (old != null)
            {
                // 既有行：沿用原账户时豁免账户有效性；更换账户仍须命中该渠道下的可用账户
                boolean sameAccount = Objects.equals(old.getSupplierAccountId(), dto.getSupplierAccountId());
                if (!sameAccount)
                {
                    if (account == null)
                    {
                        throw new ServiceException(StringUtils.format(
                                "第 {} 行渠道账户不存在或所属渠道已停用", rowNo));
                    }
                    if (!account.getSupplierId().equals(dto.getSupplierId()))
                    {
                        throw new ServiceException(StringUtils.format("第 {} 行渠道账户与所选渠道不匹配", rowNo));
                    }
                }
                else if (account != null && !account.getSupplierId().equals(dto.getSupplierId()))
                {
                    throw new ServiceException(StringUtils.format("第 {} 行渠道账户与所选渠道不匹配", rowNo));
                }
            }
            else
            {
                if (account == null)
                {
                    throw new ServiceException(StringUtils.format(
                            "第 {} 行渠道账户不存在或所属渠道已停用", rowNo));
                }
                if (!account.getSupplierId().equals(dto.getSupplierId()))
                {
                    throw new ServiceException(StringUtils.format("第 {} 行渠道账户与所选渠道不匹配", rowNo));
                }
                if (!availableKeys.contains(key))
                {
                    throw new ServiceException(StringUtils.format(
                            "第 {} 行渠道未启用或该模型未在渠道上架，禁止添加", rowNo));
                }
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
     * 明细增量对齐
     *
     * <p>以「渠道 + 模型」为业务键与既有明细比对：命中且字段有变化则按主键更新（保留明细ID），
     * 未命中的新增，既有但本次未提交的删除。相比全量替换，可保证已签约合同在履约期间
     * 变更明细后，历史对账台账 crm_contract_usage.contract_product_id 仍能指向有效明细。</p>
     *
     * @param contractId 合同ID
     * @param existing 既有明细
     * @param entities 本次提交的明细实体（已通过业务校验）
     * @return 变更统计
     */
    private SaveStat saveIncremental(Long contractId, List<CrmContractProductDto> existing,
                                     List<CrmContractProduct> entities)
    {
        SaveStat stat = new SaveStat();

        Map<String, CrmContractProductDto> existingMap = new HashMap<>();
        for (CrmContractProductDto old : existing)
        {
            if (old.getId() != null)
            {
                existingMap.put(productKey(old.getSupplierId(), old.getProductId()), old);
            }
        }

        Set<String> keepKeys = new HashSet<>();
        for (CrmContractProduct entity : entities)
        {
            String key = productKey(entity.getSupplierId(), entity.getProductId());
            keepKeys.add(key);
            CrmContractProductDto old = existingMap.get(key);
            if (old == null)
            {
                stat.toInsert.add(entity);
                continue;
            }
            if (isProductChanged(old, entity))
            {
                entity.setId(old.getId());
                crmContractProductMapper.updateContractProduct(entity);
                stat.updated++;
            }
        }

        for (CrmContractProductDto old : existing)
        {
            if (old.getId() != null && !keepKeys.contains(productKey(old.getSupplierId(), old.getProductId())))
            {
                stat.removedIds.add(old.getId());
            }
        }
        if (!stat.removedIds.isEmpty())
        {
            crmContractProductMapper.deleteContractProductByIds(contractId, stat.removedIds);
        }
        if (!stat.toInsert.isEmpty())
        {
            crmContractProductMapper.batchInsertContractProduct(stat.toInsert);
        }
        stat.inserted = stat.toInsert.size();
        stat.removed = stat.removedIds.size();
        return stat;
    }

    /**
     * 明细业务键：「渠道 + 模型」，同一模型允许挂在多个渠道下
     *
     * @param supplierId 渠道ID
     * @param productId 模型ID
     * @return 业务键
     */
    private String productKey(Long supplierId, Long productId)
    {
        return supplierId + ":" + productId;
    }

    /**
     * 判断明细是否需要更新
     *
     * <p>仅比较可变更字段，避免无实际变化的行产生无意义的日志与 update_time 漂移。</p>
     *
     * @param old 既有明细
     * @param now 本次提交的明细
     * @return true 存在差异
     */
    private boolean isProductChanged(CrmContractProductDto old, CrmContractProduct now)
    {
        return !Objects.equals(old.getSupplierAccountId(), now.getSupplierAccountId())
                || !sameDiscount(old.getCostDiscount(), now.getCostDiscount())
                || !sameDiscount(old.getSellDiscount(), now.getSellDiscount())
                || !Objects.equals(StringUtils.trimToEmpty(old.getPriceDetail()), StringUtils.trimToEmpty(now.getPriceDetail()))
                || !Objects.equals(StringUtils.trimToEmpty(old.getRemark()), StringUtils.trimToEmpty(now.getRemark()));
    }

    /**
     * 折扣比较：忽略标度差异（1.0 与 1.00 视为相同）
     *
     * @param left 折扣
     * @param right 折扣
     * @return true 数值相等
     */
    private boolean sameDiscount(BigDecimal left, BigDecimal right)
    {
        if (left == null || right == null)
        {
            return left == right;
        }
        return left.compareTo(right) == 0;
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

    /**
     * 明细增量保存统计，用于操作日志留痕
     */
    private static class SaveStat
    {
        /** 待新增明细 */
        private final List<CrmContractProduct> toInsert = new ArrayList<>();

        /** 待删除明细ID */
        private final List<Long> removedIds = new ArrayList<>();

        /** 新增条数 */
        private int inserted;

        /** 更新条数 */
        private int updated;

        /** 删除条数 */
        private int removed;
    }
}
