package com.jianke.service;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.CouponParam;
import com.jianke.entity.cart.SettlementProduct;
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

    /**
     * 是否使用商品券
     */
    private boolean isUseSingleCoupon;

    private List<CouponParam> singleCoupon;

    private Map<Long, Long> deductionMap = new HashMap<>();

    public CouponDirector() {
    }

    public CouponDirector(List<CouponParam> coupons, List<SettlementProduct> settlementProducts) {
        this.coupons = coupons;
        this.isUseSingleCoupon = coupons.stream().anyMatch(c -> c.getCouponType() == 3);
        if (isUseSingleCoupon) {
            singleCoupon = coupons.stream().filter(c -> c.getCouponType() == 3).collect(Collectors.toList());
            startCouponDeduction(settlementProducts);
        }
    }

    public long getDeductionValueByCode(Long skuCode) {
        if (!deductionMap.containsKey(skuCode)) {
            return 0L;
        }
        return deductionMap.get(skuCode);
    }

    /**
     * 订单产品优惠券摊分
     * @return
     */
    public void startCouponDeduction(List<SettlementProduct> settlementProducts) {
        if (CollectionUtils.isEmpty(singleCoupon)) {
            return;
        }
        //一张张商品券进行摊分
        for (CouponParam coupon : singleCoupon) {
            List<SettlementProduct> items = settlementProducts.stream()
                    .filter(item -> CollectionUtils.isEmpty(coupon.getUseCouponProducts())
                            || coupon.getUseCouponProducts().contains(item.getProductCode()))
                    .filter(item -> item.getCombineId() == null)
                    .collect(Collectors.toList());
            //1、计算单品金额
            long totalAmount = items.stream().mapToLong(item -> item.getActualPrice() * item.getProductNum()).sum();
            int size = items.size();
            long deductionTotalAmount = 0;
            int i = 0;

            for (SettlementProduct item : items) {
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
