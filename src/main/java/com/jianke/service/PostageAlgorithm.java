package com.jianke.service;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.cart.ShopCartBase;
import com.jianke.entity.cart.ShopCartItem;
import com.jianke.vo.DeliveryTypeVo;
import com.jianke.vo.PostageTemplateVo;
import com.jianke.vo.PostageTypeVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Slf4j
public class PostageAlgorithm {

    /**
     * 根据平台，支付类型，计算运费模板是否免邮
     * 1、先计算通用模板
     * 2、再计算特殊模板
     */
    public static boolean calPostageIsFree(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p, Integer payType, List<Long> freePostage, List<String> postageTip) {
        List<Long> itemProductCode = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).map(item -> item.getProductCode()).distinct().collect(Collectors.toList());
        for (Long productCode : itemProductCode) {
            if (freePostage.contains(productCode)) {
                log.info("客官，恭喜你买了一个免邮商品，sku={}", productCode);
                PostageTemplateVo commonTemplate = templateVos.stream().filter(t -> t.getPlatforms().contains(p)).filter(t -> t.getType() == 0).findFirst().orElse(null);
//                String freePrice = commonTemplate != null ? String.valueOf(commonTemplate.getFreePostagePrice() / 100) : "99";
//                postageTip.add(String.format("此订单满足%s元包邮。已到达包邮门槛，整单包邮。", freePrice));
                postageTip.add(String.format("此订单包含包邮商品，整单包邮。"));
                return true;
            }
        }

        List<PostageTemplateVo> templates = templateVos.stream().filter(t -> t.getPlatforms().contains(p)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(templates)) {
            log.info("平台{}查询不到对应的运费模板，使用默认的技术配置模板", p);
            return false;
        }
        boolean isFree = false;
        //在特殊模板配置过的商品，这些商品不能参与通用模板免邮计算
        List<Long> specialTemplateProduct = specialTemplateProduct(templateVos, p);
        List<Long> commonTemplateProduct = commonTemplateProduct(specialTemplateProduct, shopCartBase);
        //返回不允许包邮商品
        List<Long> unFreeProduct = unFreeProduct(templateVos, payType, p);

        //1、-----判断通用模板是否满足包邮-------
        //获取通用模板
        PostageTemplateVo commonTemplateVo = templateVos.stream().filter(t -> t.getPlatforms().contains(p)).filter(t -> t.getType() == 0).findFirst().orElse(null);
        //通用模板，此支付方式是否允许包邮
        boolean commonTemplateIsAllowFree = false;
        if (commonTemplateVo != null) {
            PostageTypeVo postageType = commonTemplateVo.getPostageTypes().stream()
                    .filter(pt -> pt.getPayType().equals(payType))
                    .filter(pt -> pt.getIsAllowFree() == 1)
                    .findAny().orElse(null);
            commonTemplateIsAllowFree = postageType != null;
            if (!commonTemplateIsAllowFree) {
                log.debug("通用模板【{}】， 不支持包邮", commonTemplateVo.getTemplateName());
            } else {
                isFree = calCommonTemplateIsFree(commonTemplateVo, shopCartBase, p, payType, specialTemplateProduct);
                if (isFree) {
                    log.info("此订单满足{}元包邮，已到达通用包邮门槛，整单包邮", commonTemplateVo.getFreePostagePrice() / 100);
                    postageTip.add(String.format("此订单满足%s元包邮。已到达包邮门槛，整单包邮。", commonTemplateVo.getFreePostagePrice() / 100));
                    return true;
                }else{
                    log.info("通用模板-此订单不满足{}元包邮", commonTemplateVo.getFreePostagePrice() / 100);
                }
            }
        }

        //2、-----判断特殊模板是否满足包邮-------
        //特殊模板按门槛由低到高排序（只对购物车商品对应的模板排序）
        List<PostageTemplateVo> sortTemplates = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getProductCodes().stream().anyMatch(code -> itemProductCode.contains(code.longValue())))
                .sorted(Comparator.comparing(PostageTemplateVo::getFreePostagePrice))
                .collect(Collectors.toList());
        Map<String, List<Integer>> templateSkuMap = sortTemplates.stream().collect(Collectors.toMap(PostageTemplateVo::getId, PostageTemplateVo::getProductCodes, (p1, p2) -> p2));
        Set<Long> allTemplateSkus = sortTemplates.stream().flatMap(t -> t.getProductCodes().stream()).map(Integer::longValue).collect(Collectors.toSet());
        //2、遍历所有特殊模板，一个个判断是否可以免邮
        Set<Integer> hasCalTemplateSkus = new HashSet<>();
        for (PostageTemplateVo templateVo : sortTemplates) {
            PostageTypeVo postageType = templateVo.getPostageTypes().stream()
                    .filter(pt -> pt.getPayType().equals(payType))
                    .filter(pt -> pt.getIsAllowFree() == 1)
                    .findAny().orElse(null);
            boolean specialTemplateIsAllowFree = postageType != null;

            if (!specialTemplateIsAllowFree) {
                log.debug("特殊模板【{}】， 不支持包邮", templateVo.getTemplateName());
                continue;
            }

            hasCalTemplateSkus.addAll(templateVo.getProductCodes());
            List<Long> higherTepmlateSkus = allTemplateSkus.stream().filter(t -> !hasCalTemplateSkus.contains(t.intValue())).collect(Collectors.toList());

            //获取购物车中，能够使用该模板计算运费的商品，
            List<ShopCartItem> items;
            if (!commonTemplateIsAllowFree) {
                //通用模板不支持包邮, 只有在该模板配置了的商品，才能去计算包邮门槛
                items = shopCartBase.getMerchants().stream()
                        .flatMap(cartItem -> cartItem.getItems().stream())
                        .filter(item -> !unFreeProduct.contains(item.getProductCode()))
                        .filter(item -> !higherTepmlateSkus.contains(item.getProductCode()))
                        .filter(item -> !commonTemplateProduct.contains(item.getProductCode()))
                        .collect(Collectors.toList());
            } else {
                //通用模板支持包邮，需要减去不包邮商品和高级特殊模板配置了的商品
                items = shopCartBase.getMerchants().stream()
                        .flatMap(cartItem -> cartItem.getItems().stream())
                        .filter(item -> !unFreeProduct.contains(item.getProductCode()))
                        .filter(item -> !higherTepmlateSkus.contains(item.getProductCode()))
                        .collect(Collectors.toList());
            }

            List<Long> skuCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());
            long itemAmount = calItemTotalNum(items);
            isFree = itemAmount >= templateVo.getFreePostagePrice();
            log.info("特殊模板【{}】，计算运费商品{}， 总金额{}分, 最低免邮金额{}分， 是否免邮={}", templateVo.getTemplateName(), skuCodes, itemAmount, templateVo.getFreePostagePrice(), isFree);
            if (isFree) {
                log.info("此订单满足{}元包邮，已到达特殊模板包邮门槛，整单包邮", templateVo.getFreePostagePrice() / 100);
                postageTip.add(String.format("此订单满足%s元包邮。已到达包邮门槛，整单包邮。", templateVo.getFreePostagePrice() / 100));
                break;
            }
        }
        return isFree;
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
        log.debug("平台{}, 特殊模板配置了不包邮的商品为：{}", p, unFreeProduct);
        return unFreeProduct;
    }

    public static List<Long> specialTemplateProduct(List<PostageTemplateVo> templateVos, String p) {
        List<Long> specialTemplateProduct = templateVos.stream()
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(p))
                .flatMap(t -> t.getProductCodes().stream())
                .map(Integer::longValue)
                .collect(Collectors.toList());
        log.info("在特殊模板配置的商品：{}", specialTemplateProduct);
        return specialTemplateProduct;
    }

    public static List<Long> commonTemplateProduct(List<Long> specialTemplateProduct, ShopCartBase shopCartBase) {
        List<Long> commonTemplate = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).map(shopCartItem -> shopCartItem.getProductCode())
                .filter(sku -> !specialTemplateProduct.contains(sku)).collect(Collectors.toList());
        log.info("可以在通用模板计算的商品：{}", commonTemplate);
        return commonTemplate;
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
    public static boolean calCommonTemplateIsFree(PostageTemplateVo commonTemplates, ShopCartBase shopCartBase, String p, Integer payType, List<Long> specialTemplateProduct) {
        //1、获取购物车中，能够使用该模板计算运费的商品（排除所有的特殊模板商品）
        List<ShopCartItem> items = shopCartBase.getMerchants().stream()
                .flatMap(cartItem -> cartItem.getItems().stream())
                .filter(item -> !specialTemplateProduct.contains(item.getProductCode()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(items)) {
            log.info("计算通用模板的商品为空");
            return false;
        }

        //2、计算是否达到 通用模板包邮门槛
        long itemAmount = calItemTotalNum(items);
        boolean isFree = itemAmount >= commonTemplates.getFreePostagePrice();
        List<Long> skuCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());
        log.info("通用模板【{}】，计算运费商品{}， 总金额{}分, 最低免邮金额{}分， 是否免邮={}", commonTemplates.getTemplateName(), skuCodes, itemAmount, commonTemplates.getFreePostagePrice(), isFree);
        return isFree;
    }

    /**
     * 根据支付方式，是否包邮，返回对应的快递方式
     * 1、不包邮的情况， 返回不包邮的快递方式中，价格最高的2个，快递类型不重复
     * 2、包邮的情况，   返回所有包邮的快递方式中， 交集最多的2个快递类型，否则返回顺丰
     */
    public static List<DeliveryTypeVo> getPostageType(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p, Integer payType, boolean isFree) {
        List<Integer> itemProductCode = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).map(item -> item.getProductCode().intValue()).distinct().collect(Collectors.toList());
        System.out.println();
        if (!isFree) {
            //根据平台，支付类型，获取通用模板和特殊模板不包邮的快递方式（特殊模板需要根据购买的商品）
            List<DeliveryTypeVo> unFreeDeliveryTypeVos = templateVos.stream()
                    .filter(t -> t.getPlatforms().contains(p))
                    .filter(t -> (t.getType() == 0) || (t.getType() == 1 && t.getProductCodes() != null && itemProductCode.stream().anyMatch(t.getProductCodes()::contains)))
                    .flatMap(t -> t.getPostageTypes().stream())
                    .filter(pt -> payType.equals(pt.getPayType()))
                    .flatMap(pt -> pt.getUnFreeDeliveryTypeVos().stream())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            log.debug("平台{}，所有模板不包邮的快递方式{}", p, JSON.toJSONString(unFreeDeliveryTypeVos));
            //选出邮费最高的2个
            List<DeliveryTypeVo> deliveryTypeVos = new ArrayList<>();
            unFreeDeliveryTypeVos.stream().sorted(Comparator.comparing(DeliveryTypeVo::getDeliveryPrice).reversed())
                    .forEach(vo -> {
                        if (deliveryTypeVos.stream().noneMatch(d -> d.getLogisticsNum().equalsIgnoreCase(vo.getLogisticsNum()))) {
                            deliveryTypeVos.add(vo);
                        }
                    });
            return deliveryTypeVos.stream().limit(2).collect(Collectors.toList());
        }

        //根据订单产品，匹配对应的模板配置（通用模板和允许包邮的特殊模板）
        List<PostageTemplateVo> commonTemplateVos = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> (t.getType() == 0) || (t.getType() == 1 && t.getProductCodes() != null && itemProductCode.stream().anyMatch(t.getProductCodes()::contains)))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                .collect(Collectors.toList());

        int size = commonTemplateVos.size();
        if (size == 0) {
            return Arrays.asList(new DeliveryTypeVo("7","顺丰", true, 0L));
        }
        List<DeliveryTypeVo> freeDeliveryTypeVos = commonTemplateVos.stream().flatMap(t -> t.getPostageTypes().stream()).filter(pt -> payType.equals(pt.getPayType())).flatMap(pt -> pt.getFreeDeliveryTypeVos().stream()).collect(Collectors.toList());
        if (size == 1) {
            return CollectionUtils.isNotEmpty(freeDeliveryTypeVos) ? freeDeliveryTypeVos : Arrays.asList(new DeliveryTypeVo("7","顺丰", true, 0L));
        }
        //选出交集数量最多的2个
        log.debug("平台{}，支付类型{}，所有模板包邮的快递方式{}", p, payType, JSON.toJSONString(freeDeliveryTypeVos));

        Map<String, Long> sortedMap = new LinkedHashMap<>();
        freeDeliveryTypeVos.stream()
                .collect(groupingBy(DeliveryTypeVo::getId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEachOrdered(e -> sortedMap.put(e.getKey(), e.getValue()));
        log.debug("所有包邮的快递方式, 计算交集并排序后的情况{}", JSON.toJSONString(sortedMap));

        //选出交集大于1的前面的两个
        Map<String, List<DeliveryTypeVo>> deliveryTypeMap = freeDeliveryTypeVos.stream().collect(groupingBy(DeliveryTypeVo::getId));
        List<DeliveryTypeVo> deliveryTypeVos = sortedMap.keySet().stream().filter(key -> sortedMap.get(key) > 1).map(key -> deliveryTypeMap.get(key).get(0)).limit(2).collect(Collectors.toList());

        //如果一个交集都没有，则返回顺丰快递（因为已经包邮了，顺丰快递兜底）
        if (deliveryTypeVos.size() == 0) {
            deliveryTypeVos.add(new DeliveryTypeVo("7","顺丰", true, 0L));
        }
        return deliveryTypeVos;
    }

    public static Map<String, String> getPostageLabel(List<PostageTemplateVo> postageTemplateList, Integer skuCode, String platform) {
        Map<String, String> map = new HashMap<>(4);
        //获取特殊模板
        PostageTemplateVo specialTemplateVo = postageTemplateList.stream().filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(platform))
                .filter(t -> t.getProductCodes().contains(skuCode))
                .findFirst().orElse(null);
        if (specialTemplateVo != null) {
            //如果允许包邮，则设置免邮门槛标签
            PostageTypeVo freePostage = specialTemplateVo.getPostageTypes().stream().filter(pt -> pt.getIsAllowFree() == 1).findFirst().orElse(null);
            if (freePostage != null) {
                map.put("postageLabel", String.format("满%s元包邮", specialTemplateVo.getFreePostagePrice() / 100));
            } else {
                //如果商品不允许包邮，则设置邮费提醒
                List<PostageTypeVo> unFreePostages = specialTemplateVo.getPostageTypes().stream().filter(pt -> pt.getIsAllowFree() == 0).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(unFreePostages)) {
                    DeliveryTypeVo deliveryTypeVo = unFreePostages.stream()
                            .flatMap(pt -> pt.getUnFreeDeliveryTypeVos().stream())
                            .sorted(Comparator.comparing(DeliveryTypeVo::getDeliveryPrice))
                            .findFirst().orElse(null);
                    if (deliveryTypeVo != null) {
                        map.put("postageDesc", String.format("特殊商品不参与包邮，在线支付运费%s元起", deliveryTypeVo.getDeliveryPrice() / 100));
                    }
                }
            }
        } else {
            //获取通用模板
            PostageTemplateVo commonTemplateVo = postageTemplateList.stream()
                    .filter(t -> t.getType() == 0)
                    .filter(t -> t.getPlatforms().contains(platform))
                    .findFirst().orElse(null);
            if (commonTemplateVo == null) {
                map.put("postageLabel", String.format("满%s元包邮", 99));
            } else {
                //如果允许包邮，则设置免邮门槛标签
                PostageTypeVo freePostage = commonTemplateVo.getPostageTypes().stream().filter(pt -> pt.getIsAllowFree() == 1).findFirst().orElse(null);
                if (freePostage != null) {
                    map.put("postageLabel", String.format("满%s元包邮", commonTemplateVo.getFreePostagePrice() / 100));
                } else {
                    //如果商品不允许包邮，则设置邮费提醒
                    List<PostageTypeVo> unFreePostages = commonTemplateVo.getPostageTypes().stream().filter(pt -> pt.getIsAllowFree() == 0).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(unFreePostages)) {
                        DeliveryTypeVo deliveryTypeVo = unFreePostages.stream()
                                .flatMap(pt -> pt.getUnFreeDeliveryTypeVos().stream())
                                .sorted(Comparator.comparing(DeliveryTypeVo::getDeliveryPrice))
                                .findFirst().orElse(null);
                        if (deliveryTypeVo != null) {
                            map.put("postageDesc", String.format("特殊商品不参与包邮，在线支付运费%s元起", deliveryTypeVo.getDeliveryPrice() / 100));
                        }
                    }
                }
            }
        }
        log.info("商品邮费标签提醒======={}", JSON.toJSONString(map));
        return map;
    }


    public static String postageDesc(List<PostageTemplateVo> templateVos, Map<Integer, String> itemProductMap, List<Integer> itemProductCode, String p, Integer payType) {
        //根据订单产品，匹配所有的允许包邮模板（通用模板和所有允许包邮的特殊模板）
        List<PostageTemplateVo> freeTemplateVos = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> (t.getType() == 0) || (t.getType() == 1 && t.getProductCodes() != null && itemProductCode.stream().anyMatch(t.getProductCodes()::contains)))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                .collect(Collectors.toList());
        //根据订单产品，匹配所有的不允许包邮模板配置
        List<PostageTemplateVo> unfreeTemplateVos = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> (t.getType() == 0) || (t.getType() == 1 && t.getProductCodes() != null && itemProductCode.stream().anyMatch(t.getProductCodes()::contains)))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> pt.getIsAllowFree() == 0))
                .collect(Collectors.toList());

        //不包邮的商品名称
        String unFreeSkuNames = null;
        if (!CollectionUtils.isEmpty(unfreeTemplateVos)) {
            unFreeSkuNames = unfreeTemplateVos.stream()
                    .flatMap(t -> t.getProductCodes().stream())
                    .filter(itemProductCode::contains)
                    .map(itemProductMap::get)
                    .collect(Collectors.joining(","));
        }

        if (freeTemplateVos.size() == 1) {
            if (unFreeSkuNames == null) {
                return String.format("全部商品需满%s元包邮，当前未满足此条件。", freeTemplateVos.get(0).getFreePostagePrice() / 100);
            } else {
                return String.format("此订单中%s不参与包邮，其余商品需满%s元包邮，当前未满足此条件。", unFreeSkuNames, freeTemplateVos.get(0).getFreePostagePrice() / 100);
            }
        }

        StringBuilder info = new StringBuilder();
        if (unFreeSkuNames != null) {
            info.append(String.format("此订单中%s不参与包邮，", unFreeSkuNames));
        }
        for (PostageTemplateVo templateVo : freeTemplateVos) {
            if (templateVo.getType() == 1) {
                String freeSkuNames = templateVo.getProductCodes().stream().filter(itemProductCode::contains).map(itemProductMap::get).collect(Collectors.joining(","));
                info.append(String.format("%s需满足%s包邮，", freeSkuNames, templateVo.getFreePostagePrice() / 100));
            } else {
               //获取通用模板可以用来计算的商品名称
                String commonSkuNames = itemProductCode.stream().filter(sku -> !templateVos.stream()
                        .filter(t -> t.getType() == 1)
                        .flatMap(t -> t.getProductCodes().stream()).collect(Collectors.toList()).contains(sku))
                        .map(itemProductMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(","));
                if (StringUtils.isNotBlank(commonSkuNames)) {
                    info.append(String.format("%s需满足%s包邮，", commonSkuNames, templateVo.getFreePostagePrice() / 100));
                }
            }
        }
        info.append("当前未满足此条件。");
        return info.toString();
    }

    public static String deliveryTypeDesc(List<PostageTemplateVo> templateVos, Map<Integer, String> itemProductMap, List<Integer> itemProductCode, String p, Integer payType, DeliveryTypeVo deliveryTypeVo) {
        //先在特殊模板找到一个包含此快递方式的商品
        Integer productCode = templateVos.stream()
                .filter(t -> t.getPlatforms().contains(p))
                .filter(t -> t.getType() == 1 && t.getProductCodes() != null && itemProductCode.stream().anyMatch(t.getProductCodes()::contains))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> pt.getUnFreeDeliveryTypeVos().stream().anyMatch(dt -> dt.getId().equals(deliveryTypeVo.getId()))))
                .flatMap(t -> t.getProductCodes().stream())
                .findFirst().orElse(null);
        if (productCode != null) {
            //特殊模板找不到，在通用模板找一个
            productCode = templateVos.stream()
                    .filter(t -> t.getType() == 1)
                    .filter(t -> t.getPlatforms().contains(p))
                    .flatMap(t -> t.getProductCodes().stream())
                    .filter(code -> !itemProductCode.contains(code))
                    .findFirst().orElse(null);
        }
        if (productCode == null) {
            return String.format("根据您选择的支付方式（在线支付）和快递方式（%s）, 收取%s元运费", deliveryTypeVo.getLogisticsName(), deliveryTypeVo.getDeliveryPrice() / 100);
        }
        return String.format("根据您选择的支付方式（在线支付）和快递方式（%s）, 按照商品%s的运费%s元收取。", deliveryTypeVo.getLogisticsName(), itemProductMap.get(productCode), deliveryTypeVo.getDeliveryPrice() / 100);
    }
}
