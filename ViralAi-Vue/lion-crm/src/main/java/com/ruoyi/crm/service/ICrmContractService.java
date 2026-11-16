package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmContractAuditVo;
import com.ruoyi.crm.dto.CrmContractCancelVo;
import com.ruoyi.crm.dto.CrmContractDto;
import com.ruoyi.crm.dto.CrmContractInitVo;
import com.ruoyi.crm.dto.CrmContractProductDto;
import com.ruoyi.crm.dto.CrmContractVo;
import com.ruoyi.crm.dto.CrmCustomerSupplierAccountVo;

/**
 * 合同Service接口
 *
 * <p>状态流转严格遵循 {@link com.ruoyi.crm.enums.ContractStatusEnum}：
 * 草稿 →（提交审批）审批中 →（通过）已签约 /（驳回）草稿；
 * 已签约 → 已到期 / 已作废 / 已归档；已到期、已作废 → 已归档；已归档为终态。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmContractService
{
    /**
     * 查询合同详情（含明细、操作日志、审批记录）
     *
     * @param id 合同主键
     * @return 合同详情
     */
    public CrmContractDto selectCrmContractById(Long id);

    /**
     * 查询合同分页列表（由 Controller 的 startPage 完成分页）
     *
     * @param query 查询条件
     * @return 合同集合
     */
    public List<CrmContractDto> selectCrmContractList(CrmContractDto query);

    /**
     * 新建合同初始化：按商机带出客户、渠道信息，此时不生成合同编号
     *
     * @param busId 商机ID
     * @return 初始化信息
     */
    public CrmContractInitVo initByBusId(Long busId);

    /**
     * 保存合同（新增或草稿更新）
     *
     * @param vo 合同入参
     * @return 合同ID
     */
    public Long saveCrmContract(CrmContractVo vo);

    /**
     * 由成交商机生成草稿合同
     *
     * <p>供商机「一键转合同」调用：不做合同主信息完整性校验（签约主体、到期时间等由销售在
     * 合同编辑页补充），仅生成编号、落库草稿并复制意向清单为合同明细。</p>
     *
     * @param busId 商机ID
     * @param products 商机意向清单快照
     * @return 新建合同ID
     */
    public Long createDraftFromBusiness(Long busId, List<CrmContractProductDto> products);

    /**
     * 提交审批：草稿 → 审批中
     *
     * @param contractId 合同ID
     * @return 结果
     */
    public int submitAudit(Long contractId);

    /**
     * 撤回审批：审批中 → 草稿（仅创建人或具备全量权限的账号可撤回）
     *
     * @param contractId 合同ID
     * @return 结果
     */
    public int withdrawAudit(Long contractId);

    /**
     * 审批处理：通过转已签约，驳回退回草稿
     *
     * @param vo 审批入参
     * @return 结果
     */
    public int audit(CrmContractAuditVo vo);

    /**
     * 合同作废：已签约 / 已到期 → 已作废
     *
     * @param vo 作废入参
     * @return 结果
     */
    public int cancel(CrmContractCancelVo vo);

    /**
     * 合同归档：已签约 / 已到期 / 已作废 → 已归档
     *
     * @param contractId 合同ID
     * @return 结果
     */
    public int archive(Long contractId);

    /**
     * 批量删除草稿合同
     *
     * @param ids 合同ID集合
     * @return 结果
     */
    public int deleteCrmContractByIds(Long[] ids);

    /**
     * 到期自动变更：将已签约且已过到期时间的合同置为已到期
     *
     * @return 变更行数
     */
    public int expireContracts();

    /**
     * 查询客户合同内可用的「渠道 + 渠道账户」组合（去重）
     *
     * <p>供用量流水人工上传等场景做三级联动：客户由合同主表绑定，渠道与账户由合同明细行承载。
     * 只统计已签约、已到期、已归档的合同，草稿/审批中/已作废不参与。</p>
     *
     * @param customerId 客户ID
     * @return 组合集合；客户不存在或没有生效合同时返回空集合
     */
    public List<CrmCustomerSupplierAccountVo> selectCustomerSupplierAccounts(Long customerId);
}
