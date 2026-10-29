package com.ruoyi.crm.dto;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * CRM模型产品对象 crm_model_product
 * <p>
 * 列表查询结果视图：厂商、创建者与更新者均以关联表的名称回显，createTime / updateTime 沿用父类字段。
 *
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmModelProductDto extends BaseEntity
{
    /** 主键 */
    private Long id;

    /** 模型厂商 */
    @Excel(name = "模型厂商")
    private String name;

    /** 模型型号 */
    @Excel(name = "模型型号")
    private String no;

    /** 模型类型1文本2多模态3图片处理4音频处理5视频处理 */
    @Excel(name = "模型类型", readConverterExp = "1=文本,2=多模态,3=图片处理,4=音频处理,5=视频处理")
    private Integer type;

    /** 模型来源 */
    @Excel(name = "模型来源")
    private String source;

    /** 价格明细 */
    @Excel(name = "价格明细")
    private String priceDetail;

    /** 状态0失效1有效 */
    @Excel(name = "状态", readConverterExp = "0=失效,1=有效")
    private Integer status;

    /** 产品描述 */
    @Excel(name = "产品描述")
    private String description;

    /** 创建者（用户名称） */
    @Excel(name = "创建者")
    private String creator;

    /** 更新者（用户名称） */
    @Excel(name = "更新者")
    private String updater;

}
