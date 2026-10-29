package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;
import com.ruoyi.crm.domain.CrmModelProduct;

/**
 * CRM模型产品表单对象
 * <p>
 * 在实体基础上挂载计费配置，使「新增产品」与「新增模型计费」在一次请求、一个事务内完成；
 * 查询详情时同样以该结构返回，billing 为 null 表示该产品尚未配置计费。
 * 由于继承自 CrmModelProduct，原有前端提交的平铺字段仍然兼容。
 *
 * @author ruoyi
 * @date 2026-10-29
 */
@Getter
@Setter
public class CrmModelProductFormDto extends CrmModelProduct
{
    private static final long serialVersionUID = 1L;

}
