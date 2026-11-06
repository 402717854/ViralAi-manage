package com.ruoyi.crm.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * 商机意向产品批量保存入参
 *
 * <p>list 为全量清单：为空数组表示清空该商机全部意向产品。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessProductBatchVo
{
    /** 商机ID */
    private Long busId;

    /** 产品清单 */
    private List<CrmBusinessProductDto> list;
}
