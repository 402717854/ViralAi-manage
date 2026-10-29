package com.ruoyi.crm.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.crm.domain.CrmBusinessProduct;
import com.ruoyi.crm.dto.BusSupModelVo;
import com.ruoyi.crm.dto.CrmBusStatusOptionDto;
import com.ruoyi.crm.dto.CrmBusinessDto;
import com.ruoyi.crm.dto.CrmBusinessStatusVo;
import com.ruoyi.crm.dto.CrmBusinessVo;
import com.ruoyi.crm.enums.BusStatusEnum;
import com.ruoyi.crm.mapper.CrmBusinessProductMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.crm.mapper.CrmBusinessMapper;
import com.ruoyi.crm.domain.CrmBusiness;
import com.ruoyi.crm.service.ICrmBusinessService;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRM 商机Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-10-28
 */
@Service
public class CrmBusinessServiceImpl implements ICrmBusinessService 
{
    @Autowired
    private CrmBusinessMapper crmBusinessMapper;

    @Autowired
    private CrmBusinessProductMapper crmBusinessProductMapper;

    /**
     * 查询CRM 商机
     * 
     * @param id CRM 商机主键
     * @return CRM 商机
     */
    @Override
    public CrmBusinessDto selectCrmBusinessById(Long id)
    {
        CrmBusiness crmBusiness = crmBusinessMapper.selectCrmBusinessById(id);
        CrmBusinessProduct crmBusinessProduct = new CrmBusinessProduct();
        crmBusinessProduct.setBusId(id);
        List<CrmBusinessProduct> crmBusinessProducts = crmBusinessProductMapper.selectCrmBusinessProductList(crmBusinessProduct);
        CrmBusinessDto crmBusinessDto = new CrmBusinessDto();
        BeanUtils.copyProperties(crmBusiness,crmBusinessDto);
        crmBusinessDto.setBusinessProducts(crmBusinessProducts);
        return crmBusinessDto;
    }

    /**
     * 查询CRM 商机列表
     * 
     * @param crmBusiness CRM 商机
     * @return CRM 商机
     */
    @Override
    public List<CrmBusinessDto> selectCrmBusinessList(CrmBusiness crmBusiness)
    {
        return crmBusinessMapper.selectCrmBusinessList(crmBusiness);
    }

    /**
     * 新增CRM 商机
     * 
     * @param crmBusinessVo CRM 商机
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCrmBusiness(CrmBusinessVo crmBusinessVo)
    {
        CrmBusiness crmBusiness=new CrmBusiness();
        BeanUtils.copyProperties(crmBusinessVo,crmBusiness);
        crmBusiness.setCreateTime(DateUtils.getNowDate());
        crmBusiness.setCreator(SecurityUtils.getLoginUser().getUserId());
        crmBusiness.setUpdater(SecurityUtils.getLoginUser().getUserId());
        int add = crmBusinessMapper.insertCrmBusiness(crmBusiness);
        List<BusSupModelVo> items = crmBusinessVo.getBusinessProducts();
        for (int i = 0; i < items.size(); i++) {
            BusSupModelVo busSupModelVo = items.get(i);
            CrmBusinessProduct crmBusinessProduct = new CrmBusinessProduct();
            crmBusinessProduct.setCreator(SecurityUtils.getLoginUser().getUserId());
            crmBusinessProduct.setCreateTime(DateUtils.getNowDate());
            crmBusinessProduct.setProductId(busSupModelVo.getProductId());
            crmBusinessProduct.setSupplierId(crmBusinessVo.getSupplierId());
            crmBusinessProduct.setBusId(crmBusiness.getId());
            crmBusinessProduct.setCostDiscount(busSupModelVo.getCostDiscount());
            crmBusinessProduct.setSellDiscount(busSupModelVo.getSellDiscount());
            crmBusinessProductMapper.insertCrmBusinessProduct(crmBusinessProduct);
        }
        return add;
    }

    /**
     * 修改CRM 商机
     * 
     * @param crmBusinessVo CRM 商机
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCrmBusiness(CrmBusinessVo crmBusinessVo)
    {
        List<BusSupModelVo> items = crmBusinessVo.getBusinessProducts();
        for (int i = 0; i < items.size(); i++) {
            BusSupModelVo busSupModelVo = items.get(i);
            CrmBusinessProduct businessProduct = crmBusinessProductMapper.selectOne(crmBusinessVo.getId(), crmBusinessVo.getSupplierId(), busSupModelVo.getProductId());
            if(businessProduct==null){
                businessProduct = new CrmBusinessProduct();
                businessProduct.setCreator(SecurityUtils.getLoginUser().getUserId());
                businessProduct.setCreateTime(DateUtils.getNowDate());
                businessProduct.setProductId(busSupModelVo.getProductId());
                businessProduct.setSupplierId(crmBusinessVo.getSupplierId());
                businessProduct.setBusId(crmBusinessVo.getId());
                businessProduct.setCostDiscount(busSupModelVo.getCostDiscount());
                businessProduct.setSellDiscount(busSupModelVo.getSellDiscount());
                crmBusinessProductMapper.insertCrmBusinessProduct(businessProduct);
            }else{
                businessProduct.setUpdator(SecurityUtils.getLoginUser().getUserId());
                businessProduct.setUpdateTime(DateUtils.getNowDate());
                businessProduct.setCostDiscount(busSupModelVo.getCostDiscount());
                businessProduct.setSellDiscount(busSupModelVo.getSellDiscount());
                crmBusinessProductMapper.update(businessProduct);
            }
        }
        CrmBusiness crmBusiness=new CrmBusiness();
        BeanUtils.copyProperties(crmBusinessVo,crmBusiness);
        // 商机状态只允许通过状态流转接口变更，编辑时不覆盖，避免状态机被绕过
        crmBusiness.setBusStatus(null);
        crmBusiness.setCreateTime(DateUtils.getNowDate());
        crmBusiness.setCreator(SecurityUtils.getLoginUser().getUserId());
        crmBusiness.setUpdater(SecurityUtils.getLoginUser().getUserId());
        return crmBusinessMapper.updateCrmBusiness(crmBusiness);
    }

    /**
     * 批量删除CRM 商机
     * 
     * @param ids 需要删除的CRM 商机主键
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCrmBusinessByIds(Long[] ids)
    {
        if (ids != null)
        {
            for (Long id : ids)
            {
                checkDeletable(id);
            }
        }
        return crmBusinessMapper.deleteCrmBusinessByIds(ids);
    }

    /**
     * 删除CRM 商机信息
     * 
     * @param id CRM 商机主键
     * @return 结果
     */
    @Override
    public int deleteCrmBusinessById(Long id)
    {
        checkDeletable(id);
        return crmBusinessMapper.deleteCrmBusinessById(id);
    }

    /**
     * 校验商机是否允许删除
     *
     * <p>提交测试及之后的状态已进入测试、成交流程，不允许删除。</p>
     *
     * @param id 商机主键
     */
    private void checkDeletable(Long id)
    {
        if (id == null)
        {
            return;
        }
        CrmBusiness current = crmBusinessMapper.selectCrmBusinessById(id);
        if (current == null)
        {
            return;
        }
        BusStatusEnum status = BusStatusEnum.of(current.getBusStatus());
        if (status == null)
        {
            throw new ServiceException(StringUtils.format("商机【{}】当前状态数据异常，无法删除", current.getName()));
        }
        if (!status.isDeletable())
        {
            throw new ServiceException(StringUtils.format("商机【{}】已处于【{}】状态，不允许删除", current.getName(),
                    status.getInfo()));
        }
    }

    /**
     * 商机状态流转
     *
     * @param statusVo 状态流转请求
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeBusStatus(CrmBusinessStatusVo statusVo)
    {
        CrmBusiness current = crmBusinessMapper.selectCrmBusinessById(statusVo.getId());
        if (current == null || Integer.valueOf(1).equals(current.getDeleted()))
        {
            throw new ServiceException("商机不存在或已删除");
        }

        BusStatusEnum currentStatus = BusStatusEnum.of(current.getBusStatus());
        if (currentStatus == null)
        {
            throw new ServiceException("商机当前状态数据异常，无法流转");
        }

        BusStatusEnum targetStatus = BusStatusEnum.of(statusVo.getBusStatus());
        if (targetStatus == null)
        {
            throw new ServiceException("目标商机状态不存在");
        }

        if (currentStatus == targetStatus)
        {
            throw new ServiceException(StringUtils.format("商机已处于【{}】状态，无需重复流转", targetStatus.getInfo()));
        }

        if (!currentStatus.canTransitTo(targetStatus))
        {
            throw new ServiceException(StringUtils.format("不允许从【{}】流转到【{}】", currentStatus.getInfo(),
                    targetStatus.getInfo()));
        }

        String failRemark = null;
        if (targetStatus == BusStatusEnum.INTERRUPTED)
        {
            if (StringUtils.isEmpty(statusVo.getFailRemark()))
            {
                throw new ServiceException("中断商机必须填写中断备注");
            }
            failRemark = statusVo.getFailRemark().trim();
        }

        // 提交测试必须先确定授信金额，金额缺失视为无法进入测试流程，禁止流转
        BigDecimal creditAmount = null;
        if (targetStatus == BusStatusEnum.SUBMIT_TEST)
        {
            if (statusVo.getCreditAmount() == null
                    || statusVo.getCreditAmount().compareTo(BigDecimal.ZERO) <= 0)
            {
                throw new ServiceException("提交测试前必须填写授信金额，且授信金额必须大于 0");
            }
            creditAmount = statusVo.getCreditAmount().setScale(2, RoundingMode.HALF_UP);
        }

        // 成交时补齐成交日期，未指定则取当前时间；流转出中断态时清空中断备注
        Date dealTime = null;
        if (targetStatus == BusStatusEnum.DEAL)
        {
            dealTime = statusVo.getDealTime() != null ? statusVo.getDealTime() : DateUtils.getNowDate();
        }

        int rows = crmBusinessMapper.updateBusStatus(statusVo.getId(), currentStatus.getCode(),
                targetStatus.getCode(), failRemark, dealTime, creditAmount, SecurityUtils.getLoginUser().getUserId());
        if (rows == 0)
        {
            throw new ServiceException("商机状态已被其他操作变更，请刷新后重试");
        }
        return rows;
    }

    /**
     * 查询商机当前状态下允许流转到的目标状态
     *
     * @param id 商机主键
     * @return 目标状态选项集合
     */
    @Override
    public List<CrmBusStatusOptionDto> selectAllowedNextStatus(Long id)
    {
        CrmBusiness current = crmBusinessMapper.selectCrmBusinessById(id);
        if (current == null || Integer.valueOf(1).equals(current.getDeleted()))
        {
            throw new ServiceException("商机不存在或已删除");
        }
        BusStatusEnum currentStatus = BusStatusEnum.of(current.getBusStatus());
        if (currentStatus == null)
        {
            return Collections.emptyList();
        }
        return currentStatus.allowedNextStatuses().stream()
                .map(status -> new CrmBusStatusOptionDto(status.getCode(), status.getInfo()))
                .collect(Collectors.toList());
    }
}
