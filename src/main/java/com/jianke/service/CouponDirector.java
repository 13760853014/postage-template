package com.jianke.service;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.CouponParam;
import com.jianke.entity.cart.ShopCartItem;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 优惠券计算管家
 */
@Data
@Slf4j
public class CouponDirector {

    /**
     * 结算使用的优惠券集合
     */
    private List<CouponParam> coupons;

    private boolean isUseCoupon;

    private boolean isContainAllActivityCoupon;

    private boolean isContainSingleCoupon;

    private List<CouponParam> allActivityCoupon;

    private List<CouponParam> singleCouponCoupon;

    private Map<Long, Long> deductionMap = new HashMap<>();

    public CouponDirector() {
    }

    public CouponDirector(List<CouponParam> coupons, List<ShopCartItem> shopCartItems) {
        this.coupons = coupons;
        this.isUseCoupon = CollectionUtils.isNotEmpty(coupons);
        this.isContainAllActivityCoupon = coupons.stream().anyMatch(c -> c.getCouponType() == 2);
        this.isContainSingleCoupon = coupons.stream().anyMatch(c -> c.getCouponType() == 3);
        if (isContainAllActivityCoupon) {
            allActivityCoupon = coupons.stream().filter(c -> c.getCouponType() == 2).collect(Collectors.toList());
        }
        if (isContainSingleCoupon) {
            singleCouponCoupon = coupons.stream().filter(c -> c.getCouponType() == 3).collect(Collectors.toList());
        }
        if (isUseCoupon) {
            startCouponDeduction(shopCartItems);
        }
    }

    /**
     * 订单产品优惠券摊分
     * @return
     */
    public void startCouponDeduction(List<ShopCartItem> shopCartItems) {
        if (CollectionUtils.isEmpty(singleCouponCoupon)) {
            return;
        }
        for (CouponParam coupon : singleCouponCoupon) {
            List<ShopCartItem> items = shopCartItems.stream()
                    .filter(item -> CollectionUtils.isEmpty(coupon.getUseCouponProducts())
                            || coupon.getUseCouponProducts().contains(item.getProductCode()))
                    .filter(item -> item.getCombineId() != null)
                    .collect(Collectors.toList());
            //1、计算单品金额
            long totalAmount = items.stream().mapToLong(item -> item.getActualPrice() * item.getProductNum()).sum();
            int size = items.size();
            long deductionTotalAmount = 0;
            int i = 0;

            for (ShopCartItem item : items) {
                i++;
                long deductionAmount = Double.valueOf(Math.floor(((double)item.getActualPrice() * item.getProductNum() / (double)totalAmount) * coupon.getCouponValue())).longValue();
                if (size == i) {
                    deductionAmount = coupon.getCouponValue() - deductionTotalAmount;
                }
                deductionTotalAmount += deductionAmount;
                if (deductionMap.get(item.getProductCode()) != null) {
                    deductionAmount = deductionMap.get(item.getProductCode()) + deductionAmount;
                }
                deductionMap.put(item.getProductCode(), deductionAmount);
            }
            log.info("购物车商品{}, 摊分优惠券{}, 摊分后结果{}", JSON.toJSONString(items), JSON.toJSONString(coupon), JSON.toJSONString(deductionMap));
        }
    }

}
