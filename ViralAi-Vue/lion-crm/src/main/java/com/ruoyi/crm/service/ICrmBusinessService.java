package com.ruoyi.crm.service;

import java.util.List;

import com.ruoyi.crm.dto.CrmBusinessDetailVo;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;

/**
 * CRM 商机Service接口
 *
 * @author ruoyi
 * @date 2026-11-04
 */
public interface ICrmBusinessService
{
    /**
     * 查询商机详情（主信息 + 意向产品 + 关联合同 + 操作日志 + 可执行动作）
     *
     * @param id 商机主键
     * @return 详情
     */
    public CrmBusinessDetailVo selectCrmBusinessById(Long id);

    /**
     * 查询商机列表
     *
     * @param query 查询条件（含数据权限）
     * @return 商机集合
     */
    public List<CrmBusinessDto> selectCrmBusinessList(CrmBusinessDto query);

    /**
     * 保存商机（id 为空新增，非空编辑）
     *
     * @param vo 商机入参
     * @return 商机ID
     */
    public Long saveCrmBusiness(CrmBusinessVo vo);

    /**
     * 商机状态流转（严格按状态机白名单校验）
     *
     * @param statusVo 状态流转请求
     * @return 结果
     */
    public int changeBusStatus(CrmBusinessStatusVo statusVo);

    /**
     * 商机转合同：成交商机复制意向清单生成草稿合同
     *
     * @param busId 商机主键
     * @return 新建合同ID
     */
    public Long convertContract(Long busId);

    /**
     * 批量删除商机（仅初始化/跟进中）
     *
     * @param ids 商机主键集合
     * @return 结果
     */
    public int deleteCrmBusinessByIds(Long[] ids);
}
