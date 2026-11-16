package com.ruoyi.crm.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.domain.CrmContract;
import com.ruoyi.crm.domain.CrmContractLog;
import com.ruoyi.crm.domain.CrmCustomer;
import com.ruoyi.crm.dto.CrmBusinessContractVo;
import com.ruoyi.crm.dto.CrmContractAuditVo;
import com.ruoyi.crm.dto.CrmContractCancelVo;
import com.ruoyi.crm.dto.CrmContractDto;
import com.ruoyi.crm.dto.CrmContractInitVo;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.dto.CrmContractVo;
import com.ruoyi.crm.dto.CrmCustomerSupplierAccountVo;
import com.ruoyi.crm.enums.BusStatusEnum;
import com.ruoyi.crm.enums.BusinessActionEnum;
import com.ruoyi.crm.enums.ContractActionEnum;
import com.ruoyi.crm.enums.ContractStatusEnum;
import com.ruoyi.crm.mapper.CrmBusinessMapper;
import com.ruoyi.crm.mapper.CrmContractLogMapper;
import com.ruoyi.crm.mapper.CrmContractMapper;
import com.ruoyi.crm.mapper.CrmContractProductMapper;
import com.ruoyi.crm.mapper.CrmCustomerMapper;
import com.ruoyi.crm.service.ICrmContractProductService;
import com.ruoyi.crm.service.ICrmContractService;
import com.ruoyi.crm.service.support.CrmBusinessLogRecorder;
import com.ruoyi.crm.service.support.CrmContractDataScope;
import com.ruoyi.crm.service.support.CrmContractNoGenerator;

/**
 * 合同Service业务层处理
 *
 * <p>状态流转与编号规则见 {@link ContractStatusEnum}、{@link CrmContractNoGenerator}。
 * 所有状态变更均带「当前状态」条件更新，避免并发覆盖；每次变更写入 crm_contract_log。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Service
public class CrmContractServiceImpl implements ICrmContractService
{
    /** 审批通过 */
    private static final int AUDIT_PASS = 1;

    /** 审批驳回 */
    private static final int AUDIT_REJECT = 2;

    /** 系统操作人名称（定时任务） */
    private static final String SYSTEM_OPERATOR = "系统";

    @Autowired
    private CrmContractMapper crmContractMapper;

    @Autowired
    private CrmContractProductMapper crmContractProductMapper;

    @Autowired
    private CrmContractLogMapper crmContractLogMapper;

    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

    @Autowired
    private CrmCustomerMapper crmCustomerMapper;

    @Autowired
    private ICrmContractProductService crmContractProductService;

    @Autowired
    private CrmContractNoGenerator crmContractNoGenerator;

    @Autowired
    private CrmContractDataScope crmContractDataScope;

    @Autowired
    private CrmBusinessLogRecorder crmBusinessLogRecorder;

    @Override
    public CrmContractDto selectCrmContractById(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(id);
        crmContractDataScope.assertViewable(contract);

        CrmContractDto dto = crmContractMapper.selectCrmContractDetail(id);
        if (dto == null)
        {
            throw new ServiceException("合同不存在或已删除");
        }
        ContractStatusEnum status = ContractStatusEnum.of(dto.getStatus());
        dto.setStatusName(status == null ? null : status.getInfo());
        dto.setProducts(crmContractProductMapper.selectContractProductList(id));
        dto.setLogs(crmContractLogMapper.selectContractLogList(id));
        dto.setAuditRecords(crmContractLogMapper.selectContractAuditList(id));
        return dto;
    }

    @Override
    public List<CrmContractDto> selectCrmContractList(CrmContractDto query)
    {
        CrmContractDto condition = query == null ? new CrmContractDto() : query;
        crmContractDataScope.applyListScope(condition);
        return crmContractMapper.selectCrmContractList(condition);
    }

    @Override
    public CrmContractInitVo initByBusId(Long busId)
    {
        if (busId == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(busId);
        if (business == null || Integer.valueOf(1).equals(business.getDeleted()))
        {
            throw new ServiceException("商机不存在或已删除");
        }
        if (business.getCustomerId() == null)
        {
            throw new ServiceException("商机未关联客户，无法创建合同");
        }
        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(business.getCustomerId());
        if (customer == null)
        {
            throw new ServiceException("商机关联的客户不存在或已删除");
        }

        CrmContractInitVo vo = new CrmContractInitVo();
        vo.setBusId(business.getId());
        vo.setBusName(business.getName());
        vo.setConsumerId(business.getCustomerId());
        vo.setConsumerName(customer.getName());
        vo.setSuggestedContractName(StringUtils.isEmpty(business.getName()) ? null : business.getName() + "合同");
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveCrmContract(CrmContractVo vo)
    {
        if (vo == null)
        {
            throw new ServiceException("合同数据不能为空");
        }
        validateMainInfo(vo);

        Long userId = SecurityUtils.getUserId();
        Date now = DateUtils.getNowDate();

        if (vo.getId() == null)
        {
            return createContract(vo, userId, now);
        }
        return updateDraftContract(vo, userId, now);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraftFromBusiness(Long busId, List<CrmContractProductDto> products)
    {
        if (busId == null)
        {
            throw new ServiceException("商机ID不能为空");
        }
        CrmBusiness business = crmBusinessMapper.selectCrmBusinessById(busId);
        if (business == null || Integer.valueOf(1).equals(business.getDeleted()))
        {
            throw new ServiceException("商机不存在或已删除");
        }
        if (business.getCustomerId() == null)
        {
            throw new ServiceException("商机未关联客户，无法生成合同");
        }

        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(business.getCustomerId());
        if (customer == null)
        {
            throw new ServiceException("商机关联的客户不存在或已删除");
        }

        String contactNo = crmContractNoGenerator.generate(business.getCustomerId());
        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();

        CrmContract entity = new CrmContract();
        entity.setBusId(busId);
        entity.setConsumerId(business.getCustomerId());
        entity.setContactNo(contactNo);
        // 草稿合同仅预填可推导信息，签约主体与到期时间由销售在合同编辑页补充
        entity.setContractName(StringUtils.substring(
                StringUtils.isEmpty(business.getName()) ? "商机合同" : business.getName() + "合同", 0, 200));
        entity.setContractEntity(StringUtils.substring(customer.getName(), 0, 200));
        entity.setStatus(ContractStatusEnum.DRAFT.getCode());
        entity.setCreator(userId);
        entity.setCreateTime(now);
        entity.setUpdater(userId);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        crmContractMapper.insertCrmContract(entity);

        int productCount = products == null ? 0 : products.size();
        writeLog(entity.getId(), ContractActionEnum.CREATE, null, ContractStatusEnum.DRAFT.getCode(),
                StringUtils.format("由商机【{}】转入，复制意向明细 {} 条，合同编号 {}",
                        business.getName(), productCount, contactNo));

        if (productCount > 0)
        {
            crmContractProductService.batchSaveContractProduct(entity.getId(), products);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int submitAudit(Long contractId)
    {
        CrmContract current = requireContract(contractId);
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status == null)
        {
            throw new ServiceException("合同状态数据异常，无法提交审批");
        }
        if (!status.isSubmittable())
        {
            throw new ServiceException(StringUtils.format("合同当前为【{}】状态，仅草稿状态可提交审批", status.getInfo()));
        }
        if (current.getExpireTime() == null)
        {
            throw new ServiceException("请先填写合同到期时间再提交审批");
        }
        int productCount = crmContractProductMapper.countContractProductByContractId(contractId);
        if (productCount <= 0)
        {
            throw new ServiceException("合同至少需要一条模型明细，请先添加模型产品");
        }

        int rows = crmContractMapper.updateContractStatus(contractId, ContractStatusEnum.DRAFT.getCode(),
                ContractStatusEnum.AUDITING.getCode(), null, null, null, null, null, null,
                SecurityUtils.getUserId(), DateUtils.getNowDate());
        if (rows == 0)
        {
            throw new ServiceException("合同状态已被其他操作变更，请刷新后重试");
        }
        writeLog(contractId, ContractActionEnum.SUBMIT, ContractStatusEnum.DRAFT.getCode(),
                ContractStatusEnum.AUDITING.getCode(), StringUtils.format("提交审批，明细 {} 条", productCount));
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int withdrawAudit(Long contractId)
    {
        CrmContract current = requireContract(contractId);
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status != ContractStatusEnum.AUDITING)
        {
            throw new ServiceException("仅审批中的合同可以撤回");
        }
        Long userId = SecurityUtils.getUserId();
        boolean isOwner = userId != null && userId.equals(current.getCreator());
        if (!isOwner && !crmContractDataScope.hasAllScope())
        {
            throw new ServiceException("仅合同创建人可以撤回审批");
        }

        int rows = crmContractMapper.updateContractStatus(contractId, ContractStatusEnum.AUDITING.getCode(),
                ContractStatusEnum.DRAFT.getCode(), null, null, null, null, null, null,
                userId, DateUtils.getNowDate());
        if (rows == 0)
        {
            throw new ServiceException("合同状态已被其他操作变更，请刷新后重试");
        }
        writeLog(contractId, ContractActionEnum.WITHDRAW, ContractStatusEnum.AUDITING.getCode(),
                ContractStatusEnum.DRAFT.getCode(), "撤回审批，合同退回草稿");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int audit(CrmContractAuditVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        Integer auditResult = vo.getAuditResult();
        if (auditResult == null || (auditResult != AUDIT_PASS && auditResult != AUDIT_REJECT))
        {
            throw new ServiceException("审批结果不合法，1 通过 / 2 驳回");
        }
        CrmContract current = requireContract(vo.getContractId());
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status != ContractStatusEnum.AUDITING)
        {
            throw new ServiceException("仅审批中的合同可以执行审批操作");
        }

        String opinion = StringUtils.trim(vo.getAuditOpinion());
        if (opinion != null && opinion.length() > 500)
        {
            throw new ServiceException("审批意见不能超过 500 个字符");
        }
        if (auditResult == AUDIT_REJECT && StringUtils.isEmpty(opinion))
        {
            throw new ServiceException("驳回时必须填写审批意见");
        }

        Date now = DateUtils.getNowDate();
        Long userId = SecurityUtils.getUserId();
        boolean pass = auditResult == AUDIT_PASS;

        int rows = crmContractMapper.updateContractStatus(vo.getContractId(), ContractStatusEnum.AUDITING.getCode(),
                pass ? ContractStatusEnum.SIGNED.getCode() : ContractStatusEnum.DRAFT.getCode(),
                now, pass ? now : null, opinion, null, null, null, userId, now);
        if (rows == 0)
        {
            throw new ServiceException("合同状态已被其他操作变更，请刷新后重试");
        }
        writeLog(vo.getContractId(), pass ? ContractActionEnum.AUDIT_PASS : ContractActionEnum.AUDIT_REJECT,
                ContractStatusEnum.AUDITING.getCode(),
                pass ? ContractStatusEnum.SIGNED.getCode() : ContractStatusEnum.DRAFT.getCode(),
                StringUtils.isEmpty(opinion) ? (pass ? "审批通过" : "审批驳回") : opinion);
        if (pass)
        {
            // 签约成功后回写商机合同状态，供商机列表「是否签合同」与结算判断使用
            syncBusinessContactStatus(current.getBusId(), "合同签约");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancel(CrmContractCancelVo vo)
    {
        if (vo == null || vo.getContractId() == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        String reason = StringUtils.trim(vo.getCancelReason());
        if (StringUtils.isEmpty(reason))
        {
            throw new ServiceException("作废原因不能为空");
        }
        if (reason.length() > 500)
        {
            throw new ServiceException("作废原因不能超过 500 个字符");
        }

        CrmContract current = requireContract(vo.getContractId());
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status == null || !status.isCancellable())
        {
            throw new ServiceException(StringUtils.format("合同当前为【{}】状态，仅已签约或已到期合同可以作废",
                    status == null ? "未知" : status.getInfo()));
        }

        Date now = DateUtils.getNowDate();
        int rows = crmContractMapper.updateContractStatus(vo.getContractId(), status.getCode(),
                ContractStatusEnum.CANCELLED.getCode(), null, null, null, now, reason, null,
                SecurityUtils.getUserId(), now);
        if (rows == 0)
        {
            throw new ServiceException("合同状态已被其他操作变更，请刷新后重试");
        }
        writeLog(vo.getContractId(), ContractActionEnum.CANCEL, status.getCode(),
                ContractStatusEnum.CANCELLED.getCode(), reason);
        // 作废后若该商机已无有效签约合同，合同状态回写为未签
        syncBusinessContactStatus(current.getBusId(), "合同作废");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int archive(Long contractId)
    {
        CrmContract current = requireContract(contractId);
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status == null || !status.isArchivable())
        {
            throw new ServiceException(StringUtils.format("合同当前为【{}】状态，仅已签约、已到期、已作废合同可以归档",
                    status == null ? "未知" : status.getInfo()));
        }

        Date now = DateUtils.getNowDate();
        int rows = crmContractMapper.updateContractStatus(contractId, status.getCode(),
                ContractStatusEnum.ARCHIVED.getCode(), null, null, null, null, null, now,
                SecurityUtils.getUserId(), now);
        if (rows == 0)
        {
            throw new ServiceException("合同状态已被其他操作变更，请刷新后重试");
        }
        writeLog(contractId, ContractActionEnum.ARCHIVE, status.getCode(),
                ContractStatusEnum.ARCHIVED.getCode(), "合同归档，归档后不可再变更");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCrmContractByIds(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            throw new ServiceException("请选择要删除的合同");
        }
        for (Long id : ids)
        {
            CrmContract current = requireContract(id);
            crmContractDataScope.assertViewable(current);
            ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
            if (status == null || !status.isDeletable())
            {
                throw new ServiceException(StringUtils.format("合同【{}】当前为【{}】状态，仅草稿可删除",
                        current.getContactNo(), status == null ? "未知" : status.getInfo()));
            }
            crmContractProductMapper.deleteContractProductByContractId(id);
            writeLog(id, ContractActionEnum.DELETE, current.getStatus(), current.getStatus(), "删除草稿合同");
        }
        return crmContractMapper.deleteCrmContractByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireContracts()
    {
        Date now = DateUtils.getNowDate();
        List<CrmContract> expiredList = crmContractMapper.selectExpiredContracts(now);
        int count = 0;
        for (CrmContract contract : expiredList)
        {
            int rows = crmContractMapper.updateExpiredContract(contract.getId(), now);
            if (rows > 0)
            {
                count += rows;
                writeSystemLog(contract.getId(), ContractStatusEnum.SIGNED.getCode(),
                        ContractStatusEnum.EXPIRED.getCode(), "合同到期，系统自动变更状态");
            }
        }
        return count;
    }

    @Override
    public List<CrmCustomerSupplierAccountVo> selectCustomerSupplierAccounts(Long customerId)
    {
        if (customerId == null)
        {
            return new ArrayList<>();
        }
        return crmContractProductMapper.selectSupplierAccountsByCustomer(customerId);
    }

    /**
     * 新建合同：生成编号、状态强制草稿
     *
     * @param vo 入参
     * @param userId 操作人
     * @param now 当前时间
     * @return 合同ID
     */
    private Long createContract(CrmContractVo vo, Long userId, Date now)
    {
        CrmBusiness business = null;
        if (vo.getBusId() != null)
        {
            business = crmBusinessMapper.selectCrmBusinessById(vo.getBusId());
            if (business == null || Integer.valueOf(1).equals(business.getDeleted()))
            {
                throw new ServiceException("商机不存在或已删除");
            }
            // 未成交商机不允许生成合同，避免跳过测试直接落合同
            BusStatusEnum busStatus = BusStatusEnum.of(business.getBusStatus());
            if (busStatus != BusStatusEnum.DEAL)
            {
                throw new ServiceException(StringUtils.format("商机当前为【{}】状态，仅成交商机可以生成合同",
                        busStatus == null ? "未知" : busStatus.getInfo()));
            }
            // 默认一商机一合同，防止重复生成
            if (crmBusinessMapper.countContractsByBusId(business.getId()) > 0)
            {
                throw new ServiceException("该商机已生成过合同，不可重复生成");
            }
        }

        Long consumerId = vo.getConsumerId() != null ? vo.getConsumerId()
                : (business == null ? null : business.getCustomerId());
        if (consumerId == null)
        {
            throw new ServiceException("客户不能为空");
        }

        CrmCustomer customer = crmCustomerMapper.selectCrmCustomerById(consumerId);
        if (customer == null)
        {
            throw new ServiceException("客户不存在或已删除");
        }

        String contactNo = crmContractNoGenerator.generate(consumerId);

        CrmContract entity = new CrmContract();
        entity.setBusId(vo.getBusId());
        entity.setConsumerId(consumerId);
        entity.setContactNo(contactNo);
        entity.setContractName(StringUtils.trim(vo.getContractName()));
        entity.setContractEntity(StringUtils.trim(vo.getContractEntity()));
        entity.setExpireTime(vo.getExpireTime());
        entity.setRemark(vo.getRemark());
        entity.setStatus(ContractStatusEnum.DRAFT.getCode());
        entity.setCreator(userId);
        entity.setCreateTime(now);
        entity.setUpdater(userId);
        entity.setUpdateTime(now);
        entity.setDeleted(0);
        crmContractMapper.insertCrmContract(entity);

        writeLog(entity.getId(), ContractActionEnum.CREATE, null, ContractStatusEnum.DRAFT.getCode(),
                StringUtils.format("新建合同，合同编号 {}", contactNo));

        if (vo.getProducts() != null)
        {
            crmContractProductService.batchSaveContractProduct(entity.getId(), vo.getProducts());
        }
        return entity.getId();
    }

    /**
     * 草稿更新：状态、编号、渠道均不可通过该接口变更
     *
     * @param vo 入参
     * @param userId 操作人
     * @param now 当前时间
     * @return 合同ID
     */
    private Long updateDraftContract(CrmContractVo vo, Long userId, Date now)
    {
        CrmContract current = requireContract(vo.getId());
        crmContractDataScope.assertViewable(current);

        ContractStatusEnum status = ContractStatusEnum.of(current.getStatus());
        if (status == null || !status.isEditable())
        {
            throw new ServiceException(StringUtils.format("合同当前为【{}】状态，仅草稿状态可以编辑",
                    status == null ? "未知" : status.getInfo()));
        }

        // 渠道、客户、商机在合同创建后锁定，只更新可编辑字段
        CrmContract update = new CrmContract();
        update.setId(vo.getId());
        update.setContractName(StringUtils.trim(vo.getContractName()));
        update.setContractEntity(StringUtils.trim(vo.getContractEntity()));
        update.setExpireTime(vo.getExpireTime());
        update.setRemark(vo.getRemark());
        update.setUpdater(userId);
        update.setUpdateTime(now);
        crmContractMapper.updateCrmContract(update);

        writeLog(vo.getId(), ContractActionEnum.UPDATE, ContractStatusEnum.DRAFT.getCode(),
                ContractStatusEnum.DRAFT.getCode(), "编辑合同主信息");

        if (vo.getProducts() != null)
        {
            crmContractProductService.batchSaveContractProduct(vo.getId(), vo.getProducts());
        }
        return vo.getId();
    }

    /**
     * 主信息校验：合同名称、签约主体、到期时间
     *
     * @param vo 入参
     */
    private void validateMainInfo(CrmContractVo vo)
    {
        String contractName = StringUtils.trim(vo.getContractName());
        if (StringUtils.isEmpty(contractName))
        {
            throw new ServiceException("合同名称不能为空");
        }
        if (contractName.length() > 200)
        {
            throw new ServiceException("合同名称不能超过 200 个字符");
        }
        String contractEntity = StringUtils.trim(vo.getContractEntity());
        if (StringUtils.isEmpty(contractEntity))
        {
            throw new ServiceException("签约主体不能为空");
        }
        if (contractEntity.length() > 200)
        {
            throw new ServiceException("签约主体不能超过 200 个字符");
        }
        if (vo.getExpireTime() == null)
        {
            throw new ServiceException("合同到期时间不能为空");
        }
        if (!vo.getExpireTime().after(DateUtils.getNowDate()))
        {
            throw new ServiceException("合同到期时间必须大于当前时间");
        }
    }

    /**
     * 查询合同并校验存在性
     *
     * @param id 合同ID
     * @return 合同
     */
    private CrmContract requireContract(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("合同ID不能为空");
        }
        CrmContract contract = crmContractMapper.selectCrmContractById(id);
        if (contract == null)
        {
            throw new ServiceException("合同不存在或已删除");
        }
        return contract;
    }

    /**
     * 回写商机合同签署状态
     *
     * <p>商机 contact_status 是结果标记：只要存在已签约或已到期合同即为已签（1），
     * 否则回写为未签（0）。合同签约、作废时触发，并写入商机操作日志。</p>
     *
     * @param busId 商机ID
     * @param reason 触发原因（写入日志）
     */
    private void syncBusinessContactStatus(Long busId, String reason)
    {
        if (busId == null)
        {
            return;
        }
        int signedCount = 0;
        List<CrmBusinessContractVo> contracts = crmBusinessMapper.selectContractsByBusId(busId);
        if (contracts != null)
        {
            for (CrmBusinessContractVo contract : contracts)
            {
                Integer status = contract.getStatus();
                if (ContractStatusEnum.SIGNED.getCode().equals(status)
                        || ContractStatusEnum.EXPIRED.getCode().equals(status))
                {
                    signedCount++;
                }
            }
        }
        int contactStatus = signedCount > 0 ? 1 : 0;
        crmBusinessMapper.updateContactStatus(busId, contactStatus);
        crmBusinessLogRecorder.record(busId, BusinessActionEnum.CONTACT, null, null,
                StringUtils.format("{}，商机合同状态更新为【{}】", reason, contactStatus == 1 ? "已签" : "未签"));
    }

    /**
     * 写入操作日志（当前登录人）
     *
     * @param contractId 合同ID
     * @param action 操作类型
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param content 操作内容
     */
    private void writeLog(Long contractId, ContractActionEnum action, Integer fromStatus, Integer toStatus,
                          String content)
    {
        CrmContractLog log = new CrmContractLog();
        log.setContractId(contractId);
        log.setAction(action.getCode());
        log.setActionName(action.getName());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setContent(StringUtils.substring(content, 0, 1000));
        log.setOperatorId(SecurityUtils.getUserId());
        log.setOperatorName(SecurityUtils.getUsername());
        log.setCreateTime(DateUtils.getNowDate());
        crmContractLogMapper.insertCrmContractLog(log);
    }

    /**
     * 写入系统操作日志（定时任务，无登录上下文）
     *
     * @param contractId 合同ID
     * @param fromStatus 变更前状态
     * @param toStatus 变更后状态
     * @param content 操作内容
     */
    private void writeSystemLog(Long contractId, Integer fromStatus, Integer toStatus, String content)
    {
        CrmContractLog log = new CrmContractLog();
        log.setContractId(contractId);
        log.setAction(ContractActionEnum.EXPIRE.getCode());
        log.setActionName(ContractActionEnum.EXPIRE.getName());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setContent(content);
        log.setOperatorName(SYSTEM_OPERATOR);
        log.setCreateTime(DateUtils.getNowDate());
        crmContractLogMapper.insertCrmContractLog(log);
    }
}
