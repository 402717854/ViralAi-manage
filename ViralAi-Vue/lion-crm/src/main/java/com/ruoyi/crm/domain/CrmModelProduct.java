package com.ruoyi.crm.domain;

import lombok.Getter;
import lombok.Setter;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * CRM模型产品对象 crm_model_product
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@Getter
@Setter
public class CrmModelProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 模型厂商 */
    private String name;

    /** 模型型号 */
    private String no;

    /** 模型类型1文本2多模态3图片处理4音频处理5视频处理 */
    private Integer type;

    /** 模型来源 */
    private String source;

    /** 价格明细 */
    private String priceDetail;

    /** 状态0失效1有效 */
    private Integer status;

    /** 产品描述 */
    private String description;

    /** 创建者 */
    private Long creator;

    /** 更新者 */
    private Long updater;

    /** 是否删除 */
    private Integer deleted;
}
