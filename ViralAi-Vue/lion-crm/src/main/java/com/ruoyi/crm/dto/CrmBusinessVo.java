package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class CrmBusinessVo {

    /** 编号 */
    private Long id;

    /** 商机名称 */
    private String name;

    /** 客户编号 */
    private Long customerId;

    /** 渠道编号 */
    private Long supplierId;

    /** 负责人 */
    private String ownerUser;

    /** 商机状态0初始化1跟进中2提交测试3测试中4成交5暂停6中断（字典 crm_bus_status） */
    private Long busStatus;

    /** 成交日期 */
    private Date dealTime;

    /** 合同状态0未签1已签 */
    private Integer contactStatus;

    /** 结算状态0未结算1已结算 */
    private Integer settleStatus;

    /** 合同金额 */
    private BigDecimal contactAmount;

    /** 授信金额 */
    private BigDecimal creditAmount;

    /** 中断备注 */
    private String failRemark;

    private List<BusSupModelVo> businessProducts;

}
