package com.ruoyi.crm.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BusSupModelVo {

    private Long productId;

    private String modelName;

    private BigDecimal costDiscount;

    private BigDecimal sellDiscount;

    private Long supplierId;

    private Long busId;


}
