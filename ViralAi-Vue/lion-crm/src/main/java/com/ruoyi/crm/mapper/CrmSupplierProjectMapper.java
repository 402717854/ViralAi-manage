package com.ruoyi.crm.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.crm.domain.CrmSupplierProject;
import com.ruoyi.crm.dto.CrmProjectChannelDto;
import com.ruoyi.crm.dto.CrmSupplierProjectDto;

/**
 * 供应商产品关联Mapper接口
 * 
 * @author ruoyi
 * @date 2026-10-29
 */
public interface CrmSupplierProjectMapper 
{
    /**
     * 查询供应商产品关联
     * 
     * @param id 供应商产品关联主键
     * @return 供应商产品关联
     */
    public CrmSupplierProject selectCrmSupplierProjectById(Long id);

    /**
     * 查询供应商产品关联列表
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 供应商产品关联集合
     */
    public List<CrmSupplierProject> selectCrmSupplierProjectList(CrmSupplierProject crmSupplierProject);

    /**
     * 新增供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    public int insertCrmSupplierProject(CrmSupplierProject crmSupplierProject);

    /**
     * 修改供应商产品关联
     * 
     * @param crmSupplierProject 供应商产品关联
     * @return 结果
     */
    public int updateCrmSupplierProject(CrmSupplierProject crmSupplierProject);

    /**
     * 删除供应商产品关联
     * 
     * @param id 供应商产品关联主键
     * @return 结果
     */
    public int deleteCrmSupplierProjectById(Long id);

    /**
     * 批量删除供应商产品关联
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCrmSupplierProjectByIds(Long[] ids);

    /**
     * 按供应商编号删除其全部产品关联
     * <p>
     * 用于供应商新增/修改时的关联全量替换，必须在同一事务内调用。
     *
     * @param supplierId 供应商编号
     * @return 结果
     */
    public int deleteCrmSupplierProjectBySupplierId(Long supplierId);

    /**
     * 按「供应商 + 模型所属供应商 + 模型」删除单条关联
     * <p>
     * 对应唯一键 uk_supplier_project (supplier_id, project_supplier_id, project_id)。
     * 供应商产品明细使用全量替换保存，本方法用于需要精确覆盖单条关联的场景。
     *
     * @param supplierId         报价归属供应商编号
     * @param projectSupplierId  模型所属供应商编号
     * @param projectId          模型编号
     * @return 结果
     */
    public int deleteCrmSupplierProjectByUniqueKey(@Param("supplierId") Long supplierId,
                                                   @Param("projectSupplierId") Long projectSupplierId,
                                                   @Param("projectId") Long projectId);

    /**
     * 按供应商编号查询其产品关联明细（含模型型号、模型厂商）
     * <p>
     * 关联 crm_model_product 取展示字段，采用左连接：模型已被删除时仍返回关联行，
     * projectNo / projectName 为 null，便于前端提示「模型已不存在」。
     *
     * @param supplierId 供应商编号
     * @return 供应商产品关联明细集合
     */
    public List<CrmSupplierProjectDto> selectSupplierProjectListBySupplierId(Long supplierId);

    /**
     * 按模型ID查询报过价的渠道（含渠道名称与折扣区间）
     * <p>
     * 供应商报价明细选择模型型号后懒加载所属渠道：按 supplier_id 聚合，
     * 折扣取区间并集；查不到记录时由前端按「当前供应商」处理。
     *
     * @param projectId 模型编号
     * @return 报价渠道集合
     */
    public List<CrmProjectChannelDto> selectChannelListByProjectId(@Param("projectId") Long projectId);
}
