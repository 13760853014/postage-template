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
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class PostageCalculateAlgorithm {

    public static void startPostageCalculate(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String platform, Integer payType, List<Long> freePostage) {
        CalculateDirector director = new CalculateDirector(templateVos, shopCartBase, platform, payType);
        boolean isFree = isContainFreeProduct(director, freePostage);
        if (!isFree) {
            isFree = calPostageIsFree(director);
        }
        List<DeliveryTypeVo> deliveryTypeVos = getPostageType(templateVos, shopCartBase, director, isFree);
        log.info("【最终结果】：  平台{}，是否包邮[{}]，返回的快递方式:\n{}\n", platform, isFree, JSON.toJSONString(deliveryTypeVos));

        if (isFree) {
            log.info("【包邮运费提示语】 {}", director.getPostageTip());
        } else {
            //不包邮的情况下，需要重新计算运费提示语
            String postageDesc = postageDesc(director);
            log.info("【不包邮运费提示语】 {}", postageDesc);
            if (deliveryTypeVos.size() == 1) {
                log.info("【不包邮不同快递运费】  根据您选择的支付方式（在线支付）和快递方式（{}）, 收取{}元运费", deliveryTypeVos.get(0).getLogisticsName(), deliveryTypeVos.get(0).getDeliveryPrice() / 100);
            } else {
                String deliveryTypeDesc = deliveryTypeDesc(director, deliveryTypeVos.get(0));
                log.info("【不包邮不同快递运费】 {}", deliveryTypeDesc);
            }
        }
        //商品在详情页展示的邮费标签
        getPostageLabel(templateVos, 80, platform);
    }

    public static boolean isContainFreeProduct(CalculateDirector director, List<Long> freePostageProducts) {
        Long productCode = director.getShopCartProductCodes().stream()
                .filter(freePostageProducts::contains)
                .findFirst().orElse(null);
        if (productCode != null) {
            log.info("客官，恭喜你买了一个免邮商品，sku={}", productCode);
            director.setPostageTip("此订单包含包邮商品，整单包邮。");
            return true;
        }
        return false;
    }

    /**
     * 根据平台，支付类型，计算运费模板是否免邮
     * 1、先计算通用模板
     * 2、再计算特殊模板
     */
    public static boolean calPostageIsFree(CalculateDirector director) {
        if (director.getCommonTemplate().getId() == null && CollectionUtils.isEmpty(director.getSpecialTemplate())) {
            log.info("平台{}查询不到对应的运费模板，使用默认的技术配置模板", director.getPlatform());
            return false;
        }

        //1、-----判断通用模板是否满足包邮-------
        //通用模板，是否允许包邮
        if (!director.isCommonTemplateAllowFree()) {
            log.debug("通用模板【{}】， 不支持包邮", director.getCommonTemplate().getTemplateName());
        } else {
            if (calCommonTemplateIsFree(director)) {
                log.info("此订单满足{}元包邮，已到达通用包邮门槛，整单包邮", director.getCommonTemplate().getFreePostagePrice() / 100);
                director.setPostageTip(String.format("此订单满足%s元包邮。已到达包邮门槛，整单包邮。", director.getCommonTemplate().getFreePostagePrice() / 100));
                return true;
            } else {
                log.info("通用模板-此订单不满足{}元包邮", director.getCommonTemplate().getFreePostagePrice() / 100);
            }
        }

        //2、-----判断特殊模板是否满足包邮-------
        //特殊模板按门槛由低到高排序（只对购物车商品对应的模板排序）
        List<PostageTemplateVo> sortTemplates = director.getSpecialTemplate().stream()
                .filter(t -> t.getProductCodes().stream().anyMatch(code -> director.getShopCartProductCodes().contains(code.longValue())))
                .sorted(Comparator.comparing(PostageTemplateVo::getFreePostagePrice))
                .collect(Collectors.toList());
        Set<Long> allTemplateSkus = sortTemplates.stream().flatMap(t -> t.getProductCodes().stream()).map(Integer::longValue).collect(toSet());
        //2、遍历所有特殊模板，一个个判断是否可以免邮
        Set<Integer> hasCalTemplateSkus = new HashSet<>();
        for (PostageTemplateVo templateVo : sortTemplates) {
            boolean isSpecialTemplateAllowFree = templateVo.getPostageTypes().stream()
                    .filter(pt -> pt.getPayType().equals(director.getPayType()))
                    .anyMatch(pt -> pt.getIsAllowFree() == 1);
            if (!isSpecialTemplateAllowFree) {
                log.debug("特殊模板【{}】， 不支持包邮", templateVo.getTemplateName());
                continue;
            }

            hasCalTemplateSkus.addAll(templateVo.getProductCodes());
            List<Long> higherTemplateSkus = allTemplateSkus.stream().filter(t -> !hasCalTemplateSkus.contains(t.intValue())).collect(Collectors.toList());

            //获取购物车中，能够使用该模板计算运费的商品，
            //1、通用模板支持包邮，需要减去不包邮商品和高级特殊模板配置了的商品
            //2、通用模板不支持包邮，需要减去不包邮商品和高级特殊模板配置了的商品，以及通用模板计算的商品
            List<ShopCartItem> items = director.getShopCartItems().stream()
                    .filter(item -> item.getCombineId() == null)
                    .filter(item -> !director.getUnFreeProduct().contains(item.getProductCode()))
                    .filter(item -> !higherTemplateSkus.contains(item.getProductCode()))
                    .filter(item -> !director.isCommonTemplateAllowFree()
                            && !director.getCommonTemplateCalculateProduct().contains(item.getProductCode()))
                    .collect(Collectors.toList());

            List<Long> skuCodes = items.stream().map(ShopCartItem::getProductCode).collect(Collectors.toList());

            //1、计算单品金额
            long itemAmount = items.stream().mapToLong(item -> item.getActualPrice() * item.getProductNum()).sum();
            //2、计算搭销金额
            if (director.isContainCombine()) {
                itemAmount = itemAmount + calCombineTotalNum(director, templateVo);
            }
            boolean isFree = itemAmount >= templateVo.getFreePostagePrice();
            log.info("特殊模板【{}】，计算运费商品{}， 总金额{}分, 最低免邮金额{}分， 是否免邮={}", templateVo.getTemplateName(), skuCodes, itemAmount, templateVo.getFreePostagePrice(), isFree);
            if (isFree) {
                log.info("此订单满足{}元包邮，已到达特殊模板包邮门槛，整单包邮", templateVo.getFreePostagePrice() / 100);
                director.setPostageTip(String.format("此订单满足%s元包邮。已到达包邮门槛，整单包邮。", templateVo.getFreePostagePrice() / 100));
                return true;
            }
        }
        return false;
    }

    //计算搭配商品的总金额
    public static long calCombineTotalNum(CalculateDirector director, PostageTemplateVo templateVo) {
        //在通用模板计算的搭销Ids
        List<Long> combineIds = director.getTemplateForCombineId().get(templateVo.getId());
        if (CollectionUtils.isNotEmpty(combineIds)) {
            long combineAmount = director.getShopCartItems().stream()
                    .filter(item -> combineIds.contains(item.getCombineId()))
                    .mapToLong(i -> i.getActualPrice() * i.getCombineNum() * i.getProductNum())
                    .sum();
            log.info("模板【{}】，计算运费的搭销Ids={}，搭销金额{}分", templateVo.getId(), combineIds, combineAmount);
            return combineAmount;
        }
        return 0L;
    }

    /**
     * 根据平台，支付类型， 计算通用模板是否免邮
     */
    public static boolean calCommonTemplateIsFree(CalculateDirector director) {
        //1、判断使用通用模板计算运费的单品（排除所有的特殊模板商品）和搭销 不能为空
        if (CollectionUtils.isEmpty(director.getCommonTemplateCalculateProduct()) && !director.isContainCombine()) {
            log.info("计算通用模板的商品为空，并且没有搭销商品");
            return false;
        }
        //2、计算单品金额
        long itemAmount = director.getShopCartItems().stream()
                .filter(item -> item.getCombineId() == null)
                .filter(item -> director.getCommonTemplateCalculateProduct().contains(item.getProductCode()))
                .mapToLong(item -> item.getActualPrice() * item.getProductNum())
                .sum();
        //3、计算搭销金额
        if (director.isContainCombine()) {
            itemAmount = itemAmount + calCombineTotalNum(director, director.getCommonTemplate());
        }
        boolean isFree = itemAmount >= director.getCommonTemplate().getFreePostagePrice();
        log.info("通用模板，计算运费的单品{}， 总金额{}分, 最低免邮金额{}分， 是否免邮={}", director.getCommonTemplateCalculateProduct(), itemAmount, director.getCommonTemplate().getFreePostagePrice(), isFree);
        return isFree;
    }

    /**
     * 根据支付方式，是否包邮，返回对应的快递方式
     * 1、不包邮的情况， 返回不包邮的快递方式中，价格最高的2个，快递类型不重复
     * 2、包邮的情况，   返回所有包邮的快递方式中， 交集最多的2个快递类型，否则返回顺丰
     */
    public static List<DeliveryTypeVo> getPostageType(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, CalculateDirector director, boolean isFree) {
        List<Integer> shopCartProductCodes = director.getShopCartProductCodes().stream().map(Long::intValue).collect(Collectors.toList());
        if (!isFree) {
            //根据平台，支付类型，获取通用模板(有通用模板计算的商品)和特殊模板不包邮的快递方式（特殊模板需要根据购买的商品）
            List<DeliveryTypeVo> unFreeDeliveryTypeVos = templateVos.stream()
                    .filter(t -> t.getPlatforms().contains(director.getPlatform()))
                    .filter(t -> (t.getType() == 0 && CollectionUtils.isNotEmpty(director.getCommonTemplateCalculateProduct()))
                            || (t.getType() == 1 && t.getProductCodes() != null && shopCartProductCodes.stream().anyMatch(t.getProductCodes()::contains)))
                    .flatMap(t -> t.getPostageTypes().stream())
                    .filter(pt -> director.getPayType().equals(pt.getPayType()))
                    .flatMap(pt -> pt.getUnFreeDeliveryTypeVos().stream())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (director.isContainCombine()) {
                List<DeliveryTypeVo> unFreeDeliveryTypes = director.getAllTemplateVos().stream()
                        .filter(t -> director.getTemplateForCombineId().keySet().contains(t.getId()))
                        .flatMap(t -> t.getPostageTypes().stream())
                        .filter(pt -> director.getPayType().equals(pt.getPayType()))
                        .flatMap(pt -> pt.getUnFreeDeliveryTypeVos().stream())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                unFreeDeliveryTypeVos.addAll(unFreeDeliveryTypes);
            }
            log.debug("平台{}，所有模板不包邮的快递方式{}", director.getPlatform(), JSON.toJSONString(unFreeDeliveryTypeVos));
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
        List<PostageTemplateVo> commonTemplateVos = director.getAllTemplateVos().stream()
                .filter(t -> (t.getType() == 0 && CollectionUtils.isNotEmpty(director.getCommonTemplateCalculateProduct()))
                        || (t.getType() == 1 && t.getProductCodes() != null && shopCartProductCodes.stream().anyMatch(t.getProductCodes()::contains))
                        || director.getTemplateForCombineId().containsKey(t.getId()))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                .collect(Collectors.toList());

        int size = commonTemplateVos.size();
        if (size == 0) {
            return Arrays.asList(new DeliveryTypeVo("7", "顺丰", true, 0L));
        }
        List<DeliveryTypeVo> freeDeliveryTypeVos = commonTemplateVos.stream().flatMap(t -> t.getPostageTypes().stream()).filter(pt -> director.getPayType().equals(pt.getPayType())).flatMap(pt -> pt.getFreeDeliveryTypeVos().stream()).collect(Collectors.toList());
        if (size == 1) {
            return CollectionUtils.isNotEmpty(freeDeliveryTypeVos) ? freeDeliveryTypeVos : Arrays.asList(new DeliveryTypeVo("7", "顺丰", true, 0L));
        }
        //选出交集数量最多的2个
        log.debug("平台{}，支付类型{}，所有模板包邮的快递方式{}", director.getPlatform(), director.getPayType(), JSON.toJSONString(freeDeliveryTypeVos));

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
        if (deliveryTypeVos.isEmpty()) {
            deliveryTypeVos.add(new DeliveryTypeVo("7", "顺丰", true, 0L));
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


    public static String postageDesc(CalculateDirector director) {
        //根据订单产品，匹配所有的允许包邮的特殊模板
        List<PostageTemplateVo> freeTemplateVos = director.getSpecialTemplate().stream()
                .filter(t -> (t.getProductCodes() != null && director.getShopCartSkuCodes().stream().anyMatch(t.getProductCodes()::contains))
                        || director.getTemplateForCombineId().keySet().contains(t.getId()))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> pt.getIsAllowFree() == 1))
                .collect(Collectors.toList());

        //根据订单产品，匹配所有的不允许包邮的特殊模板
        List<PostageTemplateVo> unfreeTemplateVos = director.getSpecialTemplate().stream()
                .filter(t -> (t.getProductCodes() != null && director.getShopCartSkuCodes().stream().anyMatch(t.getProductCodes()::contains)))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> pt.getIsAllowFree() == 0))
                .collect(Collectors.toList());

        //所有不包邮的商品名称
        String allUnFreeSkuNames = null;
        String allUnFreeCombineNames = null;
        if (!director.isCommonTemplateAllowFree()) {
            allUnFreeSkuNames = director.getShopCartSkuCodes().stream()
                    .filter(sku -> !freeTemplateVos.stream().flatMap(t -> t.getProductCodes().stream()).collect(Collectors.toList()).contains(sku))
                    .map(director.getItemProductMap()::get)
                    .collect(Collectors.joining(","));
            if (director.isContainCombine()) {
                List<Long> combineIds = director.getTemplateForCombineId().keySet().stream().filter(id -> !freeTemplateVos.stream().map(t -> t.getTemplateName()).collect(Collectors.toList()).contains(id))
                        .map(t -> director.getTemplateForCombineId().get(t))
                        .filter(Objects::nonNull).flatMap(Collection::stream).distinct().collect(Collectors.toList());
                allUnFreeCombineNames = combineIds.stream().map(id -> director.getCombineProductNameMap().get(id)).collect(Collectors.joining(","));
            }
        } else if (!CollectionUtils.isEmpty(unfreeTemplateVos)) {
            allUnFreeSkuNames = unfreeTemplateVos.stream()
                    .flatMap(t -> t.getProductCodes().stream())
                    .filter(director.getShopCartSkuCodes()::contains)
                    .map(director.getItemProductMap()::get)
                    .collect(Collectors.joining(","));
            if (director.isContainCombine()) {
                List<Long> combineIds = unfreeTemplateVos.stream()
                        .map(t -> director.getTemplateForCombineId().get(t.getTemplateName()))
                        .filter(Objects::nonNull).flatMap(Collection::stream).distinct().collect(Collectors.toList());
                allUnFreeCombineNames = combineIds.stream().map(id -> director.getCombineProductNameMap().get(id)).collect(Collectors.joining(","));
            }
        }
        if (StringUtils.isNotBlank(allUnFreeCombineNames)) {
            allUnFreeSkuNames = allUnFreeSkuNames == null ? allUnFreeCombineNames : allUnFreeSkuNames + "，" + allUnFreeCombineNames;
        }

        //只有一个模板允许包邮的情况（特殊和通用总共一个）
        if (freeTemplateVos.size() == 1 && !director.isCommonTemplateAllowFree()) {
            if (StringUtils.isBlank(allUnFreeSkuNames)) {
                return String.format("全部商品需满%s元包邮，当前未满足此条件。", freeTemplateVos.get(0).getFreePostagePrice() / 100);
            } else {
                return String.format("此订单中%s不参与包邮，其余商品需满%s元包邮，当前未满足此条件。", allUnFreeSkuNames, freeTemplateVos.get(0).getFreePostagePrice() / 100);
            }
        }

        //有多个模板允许包邮/或者全部模板不允许包邮
        StringBuilder info = new StringBuilder();
        if (StringUtils.isNotBlank(allUnFreeSkuNames)) {
            info.append(String.format("此订单中%s不参与包邮，", allUnFreeSkuNames));
        }
        for (PostageTemplateVo templateVo : freeTemplateVos) {
            String freeSkuNames = templateVo.getProductCodes().stream()
                    .filter(director.getShopCartSkuCodes()::contains)
                    .map(director.getItemProductMap()::get)
                    .collect(Collectors.joining(","));
            if (director.isContainCombine()) {
                List<Long> temp = director.getTemplateForCombineId().get(templateVo.getId());
                allUnFreeCombineNames = director.getTemplateForCombineId().get(templateVo.getId())
                        .stream()
                        .map(id -> director.getCombineProductNameMap().get(id))
                        .collect(Collectors.joining(","));
                if (StringUtils.isNotBlank(allUnFreeCombineNames)) {
                    freeSkuNames = freeSkuNames + "," + allUnFreeCombineNames;
                }
            }
            info.append(String.format("%s需满足%s包邮，", freeSkuNames, templateVo.getFreePostagePrice() / 100));
        }

        //如果通用模板可以包邮
        if (director.isCommonTemplateAllowFree()) {
            //获取通用模板可以用来计算的商品名称
            String commonSkuNames = director.getCommonTemplateCalculateProduct().stream()
                    .map(director.getItemProductMap()::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(","));
            if (director.isContainCombine()) {
                if (director.getTemplateForCombineId().keySet().contains(director.getCommonTemplate().getId())) {
                    allUnFreeCombineNames = director.getTemplateForCombineId().get(director.getCommonTemplate().getId()).stream().filter(Objects::nonNull).map(id -> director.getCombineProductNameMap().get(id)).collect(Collectors.joining(","));
                    if (StringUtils.isNotBlank(allUnFreeCombineNames)) {
                        commonSkuNames = commonSkuNames + "," + allUnFreeCombineNames;
                    }
                }
            }
            if (StringUtils.isNotBlank(commonSkuNames)) {
                info.append(String.format("%s需满足%s包邮，", commonSkuNames, director.getCommonTemplate().getFreePostagePrice() / 100));
            }
        }
        info.append("当前未满足此条件。");
        return info.toString();
    }

    public static String deliveryTypeDesc(CalculateDirector director, DeliveryTypeVo deliveryTypeVo) {
        //先在特殊模板找到一个包含此快递方式的商品
        Integer productCode = director.getSpecialTemplate().stream()
                .filter(t -> t.getProductCodes() != null && director.getShopCartSkuCodes().stream().anyMatch(t.getProductCodes()::contains))
                .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> pt.getUnFreeDeliveryTypeVos().stream().anyMatch(dt -> dt.getId().equals(deliveryTypeVo.getId()))))
                .flatMap(t -> t.getProductCodes().stream())
                .findFirst().orElse(null);
        if (productCode == null) {
            //特殊模板找不到，在通用模板找一个
            if (CollectionUtils.isNotEmpty(director.getCommonTemplateCalculateProduct())) {
                productCode = director.getCommonTemplateCalculateProduct().get(0).intValue();
            }
        }
        if (productCode == null) {
            return String.format("根据您选择的支付方式（在线支付）和快递方式（%s）, 收取%s元运费", deliveryTypeVo.getLogisticsName(), deliveryTypeVo.getDeliveryPrice() / 100);
        }
        return String.format("根据您选择的支付方式（在线支付）和快递方式（%s）, 按照商品%s的运费%s元收取。", deliveryTypeVo.getLogisticsName(), director.getItemProductMap().get(productCode), deliveryTypeVo.getDeliveryPrice() / 100);
    }
}
