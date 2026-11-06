package com.ruoyi.crm.dto;

import java.util.List;

import com.ruoyi.crm.domain.CrmBusinessLog;
import lombok.Getter;
import lombok.Setter;

/**
 * 商机详情返回对象
 *
 * <p>详情一次性返回：基础信息 + 意向产品清单（含模型名称）+ 关联合同 + 操作日志 + 当前可执行动作，
 * 前端详情页四个 Tab 与按钮区无需再发起额外请求。</p>
 *
 * @author ruoyi
 * @date 2026-11-04
 */
@Getter
@Setter
public class CrmBusinessDetailVo extends CrmBusinessDto
{
    private static final long serialVersionUID = 1L;

    /** 关联合同（0~n，默认限制一商机一合同，保留列表结构便于后续放开） */
    private List<CrmBusinessContractVo> contracts;

    /** 操作日志（按时间倒序） */
    private List<CrmBusinessLog> logs;

    /** 当前状态允许流转到的目标状态 */
    private List<CrmBusStatusOptionDto> allowedStatuses;

    /** 是否允许编辑基础信息 */
    private Boolean mainEditable;

    /** 是否允许维护意向产品清单 */
    private Boolean productEditable;

    /** 是否允许删除 */
    private Boolean deletable;

    /** 是否允许转合同 */
    private Boolean convertible;

    /** 是否允许回填结算状态 */
    private Boolean settleEditable;
}
