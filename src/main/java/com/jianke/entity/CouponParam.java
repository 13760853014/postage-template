package com.jianke.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * @tool: Created By IntellJ IDEA
 * @company: www.jianke.com
 * @author: wangzhoujie
 * @date: 2018/10/29
 * @time: 16:15
 * @description:
 */
@Getter
@Setter
@SuppressWarnings("serial")
public class CouponParam implements Serializable {
    private Long couponCode;
    private Short couponType;
    private Integer actualDiscountValue;
    private String activityName;
    private Long activityId;
    private Integer couponValue;
    private Integer minConsume;
    private List<Long> useCouponProducts;

    public CouponParam(){
    }

    public CouponParam(Integer couponType, String activityName, Integer couponValue, List<Long> useCouponProducts) {
        this.couponType = couponType.shortValue();
        this.activityName = activityName;
        this.couponValue = couponValue;
        this.useCouponProducts = useCouponProducts;
    }
}
