package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.dto.CrmBusStatusOptionDto;
import com.ruoyi.crm.dto.CrmBusinessContractVo;
import com.ruoyi.crm.dto.CrmBusinessDetailVo;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessProductDto;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.enums.BusStatusEnum;
import com.ruoyi.crm.enums.BusinessActionEnum;
import com.ruoyi.crm.enums.ContractStatusEnum;
import com.ruoyi.crm.mapper.CrmBusinessLogMapper;
import com.ruoyi.crm.mapper.CrmBusinessMapper;
import com.ruoyi.crm.mapper.CrmBusinessProductMapper;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
import com.ruoyi.crm.service.ICrmBusinessProductService;
import com.ruoyi.crm.service.ICrmBusinessService;
import com.ruoyi.crm.service.ICrmContractService;
import com.ruoyi.crm.service.support.CrmBusinessDataScope;
import com.ruoyi.crm.service.support.CrmBusinessLogRecorder;

/**
 * CRM 商机Service业务层处理
 *
 * <p>状态流转规则见 {@link BusStatusEnum}；意向产品清单在状态 &gt;= 2 后锁定；
 * 成交后可通过「一键转合同」复制产品清单生成草稿合同，商机与合同为快照关系，
 * 合同修改不会反向回写商机。所有关键动作写入 crm_business_log。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmBusinessServiceImpl implements ICrmBusinessService
{
    /** 商机名称最大长度 */
    private static final int NAME_MAX_LENGTH = 100;

    /** 合同状态：已签 */
    private static final int CONTACT_SIGNED = 1;

    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

    @Autowired
    private CrmBusinessProductMapper crmBusinessProductMapper;

    @Autowired
    private CrmBusinessLogMapper crmBusinessLogMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private ICrmBusinessProductService crmBusinessProductService;

    @Autowired
    private ICrmContractService crmContractService;

    @Autowired
    private CrmBusinessDataScope crmBusinessDataScope;

    @Autowired
    private CrmBusinessLogRecorder crmBusinessLogRecorder;

    @Override
    public CrmBusinessDetailVo selectCrmBusinessById(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(id);
        crmBusinessDataScope.assertViewable(business);

        CrmBusinessDetailVo detail = crmBusinessMapper.selectCrmBusinessDetail(id);
        if (detail == null)
        {
            throw new ServiceException("商机不存在或已删除");
        }

        BusStatusEnum status = BusStatusEnum.of(detail.getBusStatus());
        detail.setStatusName(status == null ? null : status.getInfo());
        detail.setBusinessProducts(crmBusinessProductMapper.selectBusinessProductList(id));
        detail.setLogs(crmBusinessLogMapper.selectCrmBusinessLogList(id));

        List<CrmBusinessContractVo> contracts = crmBusinessMapper.selectContractsByBusId(id);
        for (CrmBusinessContractVo contract : contracts)
        {
            ContractStatusEnum contractStatus = ContractStatusEnum.of(contract.getStatus());
            contract.setStatusName(contractStatus == null ? null : contractStatus.getInfo());
        }
        detail.setContracts(contracts);

        // 可执行动作：前端按钮与后端校验共用同一份状态机，避免两边规则漂移
        if (status == null)
        {
            detail.setAllowedStatuses(Collections.emptyList());
            detail.setMainEditable(Boolean.FALSE);
            detail.setProductEditable(Boolean.FALSE);
            detail.setDeletable(Boolean.FALSE);
            detail.setConvertible(Boolean.FALSE);
        }
        else
        {
            detail.setAllowedStatuses(status.allowedNextStatuses().stream()
                    .map(next -> new CrmBusStatusOptionDto(next.getCode(), next.getInfo()))
                    .collect(Collectors.toList()));
            detail.setMainEditable(status.isMainEditable());
            detail.setProductEditable(status.isProductEditable());
            detail.setDeletable(status.isDeletable());
            // 已生成合同的商机不再允许重复转合同（默认一商机一合同）
            boolean converted = detail.getHasContract() != null && detail.getHasContract() == 1;
            detail.setConvertible(status.isConvertible() && !converted);
        }
        return detail;
    }

    @Override
    public List<CrmBusinessDto> selectCrmBusinessList(CrmBusinessDto query)
    {
        CrmBusinessDto condition = query == null ? new CrmBusinessDto() : query;
        crmBusinessDataScope.applyListScope(condition);
        List<CrmBusinessDto> list = crmBusinessMapper.selectCrmBusinessList(condition);
        for (CrmBusinessDto row : list)
        {
            BusStatusEnum status = BusStatusEnum.of(row.getBusStatus());
            row.setStatusName(status == null ? null : status.getInfo());
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCrmBusiness(CrmBusinessVo vo)
    {
        if (vo == null)
        {
            throw new ServiceException("商机数据不能为空");
        }
        if (vo.getId() == null)
        {
            return createBusiness(vo);
        }
        return updateBusiness(vo);
    }

    /**
     * 新建商机：状态强制【初始化】，生成后渠道不可变更
     *
     * @param vo 入参
     * @return 商机ID
     */
    private Long createBusiness(CrmBusinessVo vo)
    {
        String name = validateName(vo.getName());
        validateBaseInfo(vo);

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();

        CrmBusiness entity = new CrmBusiness();
        entity.setName(name);
        entity.setCustomerId(vo.getCustomerId());
        entity.setOwnerUser(resolveOwnerUser(vo.getOwnerUser()));
        entity.setBusStatus(BusStatusEnum.INIT.getCode());
        entity.setContactStatus(0);
        entity.setContactAmount(normalizeAmount(vo.getContactAmount(), "意向合同金额"));
        entity.setCreditAmount(normalizeAmount(vo.getCreditAmount(), "授信金额"));
        entity.setRemark(vo.getRemark());
        entity.setCreator(userId);
        entity.setCreateTime(now);
        entity.setUpdater(userId);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        crmBusinessMapper.insertCrmBusiness(entity);

        crmBusinessLogRecorder.record(entity.getId(), BusinessActionEnum.CREATE, null,
                BusStatusEnum.INIT.getCode().intValue(), "新建商机，处于【初始化】状态");

        if (vo.getProducts() != null && !vo.getProducts().isEmpty())
        {
            crmBusinessProductService.batchSaveBusinessProduct(entity.getId(), vo.getProducts());
        }
        return entity.getId();
    }

    /**
     * 编辑商机：仅【初始化】【跟进中】允许，渠道锁定不可修改
     *
     * @param vo 入参
     * @return 商机ID
     */
    private Long updateBusiness(CrmBusinessVo vo)
    {
        CrmBusiness current = requireBusiness(vo.getId());
        crmBusinessDataScope.assertViewable(current);

        BusStatusEnum status = BusStatusEnum.of(current.getBusStatus());
        if (status == null)
        {
            throw new ServiceException("商机状态数据异常，无法编辑");
        }
        if (!status.isMainEditable())
        {
            throw new ServiceException(StringUtils.format("商机当前为【{}】状态，仅初始化/跟进中允许编辑",
                    status.getInfo()));
        }
        // 商机不再绑定渠道，渠道由明细行决定，因此这里只校验客户与金额
        String name = validateName(vo.getName());
        validateBaseInfo(vo);

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();

        CrmBusiness update = new CrmBusiness();
        update.setId(vo.getId());
        update.setName(name);
        update.setCustomerId(vo.getCustomerId());
        update.setOwnerUser(resolveOwnerUser(vo.getOwnerUser()));
        update.setContactAmount(normalizeAmount(vo.getContactAmount(), "意向合同金额"));
        update.setCreditAmount(normalizeAmount(vo.getCreditAmount(), "授信金额"));
        update.setRemark(vo.getRemark());
        update.setUpdater(userId);
        update.setUpdateTime(now);
        crmBusinessMapper.updateCrmBusiness(update);

        crmBusinessLogRecorder.record(vo.getId(), BusinessActionEnum.UPDATE,
                status.getCode().intValue(), status.getCode().intValue(), "编辑商机基础信息");

        if (vo.getProducts() != null)
        {
            crmBusinessProductService.batchSaveBusinessProduct(vo.getId(), vo.getProducts());
        }
        return vo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeBusStatus(CrmBusinessStatusVo statusVo)
    {
        if (statusVo == null || statusVo.getId() == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness current = requireBusiness(statusVo.getId());
        crmBusinessDataScope.assertViewable(current);

        BusStatusEnum currentStatus = BusStatusEnum.of(current.getBusStatus());
        if (currentStatus == null)
        {
            throw new ServiceException("商机当前状态数据异常，无法流转");
        }
        BusStatusEnum targetStatus = BusStatusEnum.of(statusVo.getBusStatus());
        if (targetStatus == null)
        {
            throw new ServiceException("目标商机状态不存在");
        }
        if (currentStatus == targetStatus)
        {
            throw new ServiceException(StringUtils.format("商机已处于【{}】状态，无需重复流转", targetStatus.getInfo()));
        }
        if (!currentStatus.canTransitTo(targetStatus))
        {
            throw new ServiceException(StringUtils.format("不允许从【{}】流转到【{}】", currentStatus.getInfo(),
                    targetStatus.getInfo()));
        }

        // 中断必须说明原因，且为不可恢复终态
        String failRemark = null;
        if (targetStatus == BusStatusEnum.INTERRUPTED)
        {
            String remark = StringUtils.trim(statusVo.getFailRemark());
            if (StringUtils.isEmpty(remark))
            {
                throw new ServiceException("中断商机必须填写中断备注");
            }
            if (remark.length() > 255)
            {
                throw new ServiceException("中断备注不能超过 255 个字符");
            }
            failRemark = remark;
        }

        // 成交前必须已有意向报价清单，否则转合同会产生空合同
        if (targetStatus == BusStatusEnum.DEAL)
        {
            if (crmBusinessProductMapper.countBusinessProductByBusId(statusVo.getId()) <= 0)
            {
                throw new ServiceException("成交前请先维护意向模型产品清单");
            }
        }

        Date dealTime = targetStatus == BusStatusEnum.DEAL
                ? (statusVo.getDealTime() != null ? statusVo.getDealTime() : DateUtils.getNowDate())
                : null;
        BigDecimal creditAmount = normalizeAmount(statusVo.getCreditAmount(), "授信金额");

        int rows = crmBusinessMapper.updateBusStatus(statusVo.getId(), currentStatus.getCode(),
                targetStatus.getCode(), failRemark, dealTime, creditAmount, SecurityUtils.getUserId());
        if (rows == 0)
        {
            throw new ServiceException("商机状态已被其他操作变更，请刷新后重试");
        }

        String content = StringUtils.format("状态由【{}】变更为【{}】", currentStatus.getInfo(), targetStatus.getInfo());
        if (StringUtils.isNotEmpty(failRemark))
        {
            content = content + "，原因：" + failRemark;
        }
        crmBusinessLogRecorder.record(statusVo.getId(), BusinessActionEnum.STATUS,
                currentStatus.getCode().intValue(), targetStatus.getCode().intValue(), content);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertContract(Long busId)
    {
        if (busId == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        // 行锁：同一商机并发点「转合同」时串行执行，避免生成多份合同
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessByIdForUpdate(busId);
        crmBusinessDataScope.assertViewable(business);

        BusStatusEnum status = BusStatusEnum.of(business.getBusStatus());
        if (status == null || !status.isConvertible())
        {
            throw new ServiceException(StringUtils.format("商机当前为【{}】状态，仅成交商机可以转合同",
                    status == null ? "未知" : status.getInfo()));
        }
        int existsCount = crmBusinessMapper.countContractsByBusId(busId);
        if (existsCount > 0)
        {
            throw new ServiceException("该商机已生成过合同，不可重复转合同");
        }

        List<CrmBusinessProductDto> products = crmBusinessProductMapper.selectBusinessProductList(busId);
        if (products == null || products.isEmpty())
        {
            throw new ServiceException("商机无意向模型产品，无法生成合同");
        }

        // 复制一份快照到合同明细，后续商机或合同任一方的变更互不影响
        List<CrmContractProductDto> snapshot = new ArrayList<>(products.size());
        for (CrmBusinessProductDto product : products)
        {
            CrmContractProductDto target = new CrmContractProductDto();
            target.setSupplierId(product.getSupplierId());
            target.setSupplierAccountId(product.getSupplierAccountId());
            target.setProductId(product.getProductId());
            target.setCostDiscount(product.getCostDiscount());
            target.setSellDiscount(product.getSellDiscount());
            target.setPriceDetail(product.getPriceDetail());
            snapshot.add(target);
        }

        Long contractId = crmContractService.createDraftFromBusiness(busId, snapshot);
        crmBusinessLogRecorder.record(busId, BusinessActionEnum.CONVERT,
                status.getCode().intValue(), status.getCode().intValue(),
                StringUtils.format("转合同，复制意向明细 {} 条，生成合同ID {}", snapshot.size(), contractId));
        return contractId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCrmBusinessByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的商机");
        }
        for (Long id : ids)
        {
            CrmBusiness current = requireBusiness(id);
            crmBusinessDataScope.assertViewable(current);

            BusStatusEnum status = BusStatusEnum.of(current.getBusStatus());
            if (status == null || !status.isDeletable())
            {
                throw new ServiceException(StringUtils.format("商机【{}】当前为【{}】状态，仅初始化/跟进中允许删除",
                        current.getName(), status == null ? "未知" : status.getInfo()));
            }
            if (crmBusinessMapper.countContractsByBusId(id) > 0)
            {
                throw new ServiceException(StringUtils.format("商机【{}】已生成合同，不允许删除", current.getName()));
            }
            // 商机产品随商机一并清理，避免残留孤立报价数据
            crmBusinessProductMapper.deleteBusinessProductByBusId(id);
            crmBusinessLogRecorder.record(id, BusinessActionEnum.DELETE,
                    status.getCode().intValue(), status.getCode().intValue(), "删除商机");
        }
        return crmBusinessMapper.deleteCrmBusinessByIds(ids);
    }

    /**
     * 查询商机并校验存在性
     *
     * @param id 商机ID
     * @return 商机
     */
    private CrmBusiness requireBusiness(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(id);
        if (business == null)
        {
            throw new ServiceException("商机不存在或已删除");
        }
        return business;
    }

    /**
     * 商机名称校验
     *
     * @param name 名称
     * @return 去空格后的名称
     */
    private String validateName(String name)
    {
        String value = StringUtils.trim(name);
        if (StringUtils.isEmpty(value))
        {
            throw new ServiceException("商机名称不能为空");
        }
        if (value.length() > NAME_MAX_LENGTH)
        {
            throw new ServiceException(StringUtils.format("商机名称不能超过 {} 个字符", NAME_MAX_LENGTH));
        }
        return value;
    }

    /**
     * 基础信息校验：客户必填且存在
     * <p>
     * 商机不再直接绑定渠道（客户 1:N 渠道，渠道由明细行承载），
     * 因此这里只校验客户；产品与渠道的归属在明细保存时校验。
     *
     * @param vo 入参
     */
    private void validateBaseInfo(CrmBusinessVo vo)
    {
        if (vo.getCustomerId() == null)
        {
            throw new ServiceException("客户不能为空");
        }
        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(vo.getCustomerId());
        if (customer == null)
        {
            throw new ServiceException("客户不存在或已删除");
        }
    }

    /**
     * 负责人：未填写时默认取当前登录账号
     *
     * @param ownerUser 入参负责人
     * @return 负责人账号
     */
    private String resolveOwnerUser(String ownerUser)
    {
        String value = StringUtils.trim(ownerUser);
        return StringUtils.isEmpty(value) ? SecurityUtils.getUsername() : value;
    }

    /**
     * 金额规范化：空值返回 null，负数或超过两位小数直接拒绝
     *
     * @param amount 金额
     * @param label 字段名称
     * @return 保留两位小数的金额
     */
    private BigDecimal normalizeAmount(BigDecimal amount, String label)
    {
        if (amount == null)
        {
            return null;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException(label + "不能为负数");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
