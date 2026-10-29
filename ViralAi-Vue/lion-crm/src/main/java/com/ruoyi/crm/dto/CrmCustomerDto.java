package com.ruoyi.crm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.crm.domain.CrmCustomer;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CrmCustomerDto{

    private Long id;

    /** 客户名称 */
    @Excel(name = "客户名称")
    private String name;

    /** 负责人 */
    @Excel(name = "负责人")
    private String ownerUser;

    /** 客户状态 */
    @Excel(name = "客户状态")
    private Integer status;

    /** 手机 */
    @Excel(name = "手机")
    private String mobile;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 详细地址 */
    @Excel(name = "详细地址")
    private String detailAddress;

    /** 所属行业 */
    @Excel(name = "所属行业")
    private String industry;

    /** 创建者 */
    @Excel(name = "创建者")
    private String creator;

    /** 更新者 */
    @Excel(name = "更新者")
    private String updater;

    /** 是否删除 */
    @Excel(name = "是否删除")
    private Integer deleted;

    private Date createTime;

    private Date updateTime;
}
