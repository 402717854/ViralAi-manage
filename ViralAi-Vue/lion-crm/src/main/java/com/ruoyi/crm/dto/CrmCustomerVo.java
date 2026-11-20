package com.ruoyi.crm.dto;

import com.ruoyi.crm.domain.CrmCustomer;
import lombok.Getter;
import lombok.Setter;

/**
 * 客户保存对象
 * <p>
 * 客户资料不再维护关联渠道：渠道与模型的关系由商机、合同明细行承载，
 * 保存这些明细时由服务端做强制性一致性校验。该类保留用于后续客户扩展字段。
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmCustomerVo extends CrmCustomer
{
    private static final long serialVersionUID = 1L;
}
