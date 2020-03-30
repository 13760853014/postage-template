package com.jianke.service;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.Coupon;
import com.jianke.entity.cart.ShopCartBase;
import com.jianke.entity.cart.ShopCartItem;
import com.jianke.vo.DeliveryTypeVo;
import com.jianke.vo.PostageTemplateVo;
import com.jianke.vo.PostageTypeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class PostageAlgorithm {

    /**
     * 根据平台，支付类型，计算运费模板是否免邮
     * 1、先计算通用模板
     * 2、再计算特殊模板
     */
    public static boolean calPostageIsFree(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p, Integer payType, List<Coupon> coupons) {
        List<PostageTemplateVo> templates = templateVos.stream().filter(t -> t.getPlatforms().contains(p)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templates)) {
            log.info("平台{}查询不到对应的运费模板，使用默认的技术配置模板", p);
            return false;
        }
        boolean isFree = false;
        //在特殊模板配置过的商品，这些商品不能参与通用模板免邮计算
        List<Long> unFreeProduct = unFreeProduct(templateVos, payType, p);

        //1、-----判断通用模板是否满足包邮-------
        //获取通用模板
        PostageTemplateVo commonTemplateVo = templateVos.stream().filter(t -> t.getPlatforms().contains(p)).filter(t -> t.getType() == 0).findFirst().orElse(null);
        //通用模板，此支付方式是否允许包邮
        boolean commonTemplateIsAllowFree = false;
        if (commonTemplateVo != null) {
            PostageTypeVo postageType = commonTemplateVo.getPostageTypes().stream()
                    .filter(pt -> pt.getPayType() == 1)
                    .filter(pt -> pt.getIsAllowFree() == 1)
                    .findAny().orElse(null);
            commonTemplateIsAllowFree = postageType != null;
            if (!commonTemplateIsAllowFree) {
                log.debug("通用模板【{}】, 支付方式{}， 不支持包邮", commonTemplateVo.getTemplateName(), payType);
            } else {
                isFree = calCommonTemplateIsFree(commonTemplateVo, shopCartBase, p, payType, coupons, unFreeProduct);
                if (isFree) {
                    log.info("此订单满足{}元包邮，已到达包邮门槛，整单包邮", commonTemplateVo.getFreePostagePrice() / 100);
                    return true;
                }
            }
        }

        //2、-----判断特殊模板是否满足包邮-------
        //特殊模板按门槛由低到高排序
        List<PostageTemplateVo> sortTemplates = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> t.getType() == 1)
                .sorted(Comparator.comparing(PostageTemplateVo::getFreePostagePrice))
                .collect(Collectors.toList());
        Map<String, List<Integer>> templateSkuMap = sortTemplates.stream().collect(Collectors.toMap(PostageTemplateVo::getId, PostageTemplateVo::getProductCodes));
        Set<Long> allTemplateSkus = sortTemplates.stream().flatMap(t -> t.getProductCodes().stream()).map(Integer::longValue).collect(Collectors.toSet());
        //2、遍历所有特殊模板，一个个判断是否可以免邮
        Set<Integer> hasCalTemplateSkus = new HashSet<>();
        for (PostageTemplateVo templateVo : sortTemplates) {
            PostageTypeVo postageType = commonTemplateVo.getPostageTypes().stream()
                    .filter(pt -> pt.getPayType() == 1)
                    .filter(pt -> pt.getIsAllowFree() == 1)
                    .findAny().orElse(null);
            boolean specialTemplateIsAllowFree = postageType != null;
            if (!specialTemplateIsAllowFree) {
                log.debug("特殊模板【{}】, 支付方式{}， 不支持包邮", templateVo.getTemplateName(), payType);
                continue;
            }
            hasCalTemplateSkus.addAll(templateVo.getProductCodes());

            //获取购物车中，能够使用该模板计算运费的商品，
            List<ShopCartItem> items;
            if (!commonTemplateIsAllowFree) {
                //通用模板不支持包邮, 只有在该模板配置了的商品，才能去计算包邮门槛
                items = shopCartBase.getMerchants().stream()
                        .flatMap(cartItem -> cartItem.getItems().stream())
                        .filter(item -> templateVo.getProductCodes().contains(item.getProductCode().intValue()))
                        .filter(item -> !unFreeProduct.contains(item.getProductCode()))
                        .collect(Collectors.toList());
            } else {
                //通用模板支持包邮，需要减去不包邮商品和高级特殊模板配置了的商品
                List<Long> higherTepmlateSkus = allTemplateSkus.stream().filter(t -> hasCalTemplateSkus.contains(t)).collect(Collectors.toList());
                items = shopCartBase.getMerchants().stream()
                        .flatMap(cartItem -> cartItem.getItems().stream())
                        .filter(item -> !unFreeProduct.contains(item.getProductCode()))
                        .filter(item -> !higherTepmlateSkus.contains(item.getProductCode()))
                        .collect(Collectors.toList());
            }

            List<Long> skuCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());
            long itemAmount = calItemTotalNum(items);
            long couponValue = templateUseCouponAmount(items, coupons);
            isFree = itemAmount - couponValue > templateVo.getFreePostagePrice();
            log.info("特殊模板【{}】，计算运费商品{}， 总金额{}, 可用优惠券金额{}, 最低免邮金额{}， 是否免邮={}", templateVo.getTemplateName(), skuCodes, itemAmount, couponValue, templateVo.getFreePostagePrice(), isFree);
            if (isFree) {
                log.info("此订单满足{}元包邮，已到达包邮门槛，整单包邮", templateVo.getFreePostagePrice() / 100);
                break;
            }
        }
        return isFree;
    }

    public static void postageDesc(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p, Integer payType) {
        List<PostageTemplateVo> templateVoList = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p)).collect(Collectors.toList());
        if (templateVoList.size() == 1) {
            log.info("不包邮金额说明: 全部商品需满{}元包邮，当前未满足此条件。", templateVoList.get(0).getFreePostagePrice() / 100);
            return;
        }

        StringBuilder info = new StringBuilder();
        List<Long> unFreeProduct = unFreeProduct(templateVos, payType, p);
        if (!CollectionUtils.isEmpty(unFreeProduct)) {
            Map<Long, String> itemMap = shopCartBase.getMerchants().stream()
                    .flatMap(shopCart -> shopCart.getItems().stream())
                    .collect(Collectors.toMap(item -> item.getProductCode(), item -> item.getProductName(), (i1, i2) -> i2));
            info.append("此订单中");
            for (Long skuCode : unFreeProduct) {
                info.append("此订单中" + itemMap.get(skuCode) + ",不参与包邮");
            }
            info.append("不参与包邮");
        }

        //1、筛选特殊配置模板
        List<PostageTemplateVo> specialTemplates = templateVos.stream()
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(p))
                .sorted(Comparator.comparing(PostageTemplateVo::getFreePostagePrice))
                .collect(Collectors.toList());

        for (PostageTemplateVo templateVo : specialTemplates) {

        }

    }

    //特殊模板，配置了该平台，该支付方式下，不能参与免邮计算的商品
    public static List<Long> unFreeProduct(List<PostageTemplateVo> templateVos, Integer payType, String p) {
        List<Long> unFreeProduct = templateVos.stream()
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> t.getPostageTypes().stream()
                        .filter(pt -> payType.equals(pt.getPayType()))
                        .anyMatch(pt -> pt.getIsAllowFree() == 0))
                .flatMap(t -> t.getProductCodes().stream())
                .map(Integer::longValue)
                .collect(Collectors.toList());
        log.debug("平台{}, 支付方式{}, 特殊模板配置了不包邮的商品为：{}", p, payType, unFreeProduct);
        return unFreeProduct;
    }

    public static List<Long> specialTemplateProduct(List<PostageTemplateVo> templateVos, String p) {
        List<Long> specialTemplateProduct = templateVos.stream()
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(p))
                .flatMap(t -> t.getProductCodes().stream())
                .map(sku -> sku.longValue())
                .collect(Collectors.toList());
        log.info("在特殊模板配置的商品：{}", specialTemplateProduct);
        return specialTemplateProduct;
    }

    /**
     * 该特殊配置模板能够使用优惠券的金额
     * @param items
     * @param coupons
     * @return
     */
    public static long templateUseCouponAmount(List<ShopCartItem> items, List<Coupon> coupons) {
        if (CollectionUtils.isEmpty(coupons)) {
            return 0;
        }
        long useAmount = 0;
        List<Long> itemCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());
        for (Coupon coupon : coupons) {
            if (coupon.getType() == 1) {
                useAmount = useAmount + coupon.getCouponValue();
            } else {
                //购物车计算该特殊配置模板的商品，是否包含在商品券中
                List<Long> contains = itemCodes.stream().filter(sku -> coupon.getProductCodes().contains(sku)).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(contains)) {
                    useAmount = useAmount + coupon.getCouponValue();
                    log.info("购物车商品{}，可以使用商品券{}", contains, coupons);
                }
            }
        }
        return useAmount;
    }

    public static long calItemTotalNum(List<ShopCartItem> items) {
        long sum = 0;
        for (ShopCartItem item : items) {
            if (item.getCombineId() == null) {
                sum = sum + item.getActualPrice() * item.getProductNum();
            } else {
                sum = sum + item.getActualPrice() * item.getCombineNum() * item.getProductNum();
            }
        }
        return sum;
    }

    /**
     * 根据平台，支付类型， 计算通用模板是否免邮
     */
    public static boolean calCommonTemplateIsFree(PostageTemplateVo commonTemplates, ShopCartBase shopCartBase, String p, Integer payType, List<Coupon> coupons, List<Long> unFreeProduct) {
        //1、获取购物车中，能够使用该模板计算运费的商品（排除所有的特殊模板商品）
        List<ShopCartItem> items = shopCartBase.getMerchants().stream()
                .flatMap(cartItem -> cartItem.getItems().stream())
                .filter(item -> !unFreeProduct.contains(item.getProductCode()))
                .collect(Collectors.toList());

        //2、计算是否达到 通用模板包邮门槛
        long itemAmount = calItemTotalNum(items);
        long couponValue = templateUseCouponAmount(items, coupons);
        boolean isFree = itemAmount - couponValue > commonTemplates.getFreePostagePrice();
        List<Long> skuCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());
        log.info("通用模板【{}】，计算运费商品{}， 总金额{}, 可用优惠券金额{}, 最低免邮金额{}， 是否免邮={}", commonTemplates.getTemplateName(), skuCodes, itemAmount, couponValue, commonTemplates.getFreePostagePrice(), isFree);
        return isFree;
    }

    /**
     * 根据支付方式，是否包邮，返回对应的快递方式
     * 1、不包邮的情况， 返回不包邮的快递方式中，价格最高的2个，快递类型不重复
     * 2、包邮的情况，   返回所有包邮的快递方式中， 交集最多的2个快递类型，否则返回顺丰
     */
    public static List<DeliveryTypeVo> getPostageType(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p, Integer payType, boolean isFree) {
        System.out.println();
        List<DeliveryTypeVo> returnTypes = new ArrayList<>();
        if (!isFree) {
            //根据平台，支付类型，获取所有模板不包邮的快递方式
            List<DeliveryTypeVo> unFreeDeliveryTypeVos = new ArrayList<>();
            for(PostageTemplateVo vo : templateVos) {
                if (!vo.getPlatforms().contains(p)) {
                    continue;
                }
                for (PostageTypeVo type : vo.getPostageTypes()) {
                    if (payType.equals(type.getPayType()) && type.getIsAllowFree() == 1) {
                        unFreeDeliveryTypeVos.addAll(type.getUnFreeDeliveryTypeVos());
                    }
                }
            }
            log.debug("平台{}，支付类型{}，所有模板不包邮的快递方式{}", p, payType, JSON.toJSONString(unFreeDeliveryTypeVos));
            //选出邮费最高的2个
            unFreeDeliveryTypeVos.sort(Comparator.comparing(DeliveryTypeVo::getDeliveryPrice).reversed());
            for (DeliveryTypeVo vo : unFreeDeliveryTypeVos) {
                if (returnTypes.size() >= 2) {
                    return returnTypes;
                }
                //是否存在此快递方式
                DeliveryTypeVo deliveryTypeVo = returnTypes.stream().filter(d -> d.getLogisticsNum().equals(vo.getLogisticsNum())).findFirst().orElse(null);
                if (deliveryTypeVo == null) {
                    returnTypes.add(vo);
                }
            }
        } else {
            List<DeliveryTypeVo> freeDeliveryTypeVos = new ArrayList<>();
            for(PostageTemplateVo vo : templateVos) {
                if (!vo.getPlatforms().contains(p)) {
                    continue;
                }
                for (PostageTypeVo type : vo.getPostageTypes()) {
                    if (payType.equals(type.getPayType()) && type.getIsAllowFree() == 1) {
                        freeDeliveryTypeVos.addAll(type.getFreeDeliveryTypeVos());
                    }
                }
            }
            log.debug("平台{}，支付类型{}，所有模板包邮的快递方式{}", p, payType, JSON.toJSONString(freeDeliveryTypeVos));
            Map<String, Long> map = freeDeliveryTypeVos.stream().collect(groupingBy(DeliveryTypeVo::getId, Collectors.counting()));
            List<String> deliveryTypeIds = map.keySet().stream().filter(key -> map.get(key) > 1L).limit(2).collect(Collectors.toList());
            Map<String, List<DeliveryTypeVo>> deliveryTypeMap = freeDeliveryTypeVos.stream().collect(groupingBy(DeliveryTypeVo::getId));

            //选出交集最多的2个
            if (deliveryTypeIds.size() == 2) {
                for (String id : deliveryTypeIds) {
                    returnTypes.add(deliveryTypeMap.get(id).get(0));
                }
            } else if (deliveryTypeIds.size() == 1) {
                String id = deliveryTypeIds.get(0);
                returnTypes.add(deliveryTypeMap.get(id).get(0));
                returnTypes.add(new DeliveryTypeVo("5","顺丰", true, 0L));
            } else {
                returnTypes.add(new DeliveryTypeVo("5","顺丰", true, 0L));
            }
        }
        return returnTypes;
    }
}
