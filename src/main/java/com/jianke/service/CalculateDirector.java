package com.jianke.service;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.cart.ShopCartBase;
import com.jianke.entity.cart.ShopCartItem;
import com.jianke.vo.PostageTemplateVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * 运费计算管家
 */
@Data
@Slf4j
public class CalculateDirector implements Serializable {
    /**
     * 购物车单品集合（不包括搭销）
     */
    private List<Long> shopCartProductCodes;

    /**
     * 购物车搭销商品集合
     */
    private List<Long> shopCartCombineProductCodes;

    private List<Integer> shopCartSkuCodes;

    /**
     * 在特殊模板配置过的商品，这些商品不能参与通用模板免邮计算
     */
    private List<Long> specialTemplateCalculateProduct = Collections.emptyList();

    /**
     * 不在特殊模板的商品，用通用模板计算
     */
    private List<Long> commonTemplateCalculateProduct;

    /**
     * 不允许包邮商品
     */
    private List<Long> unFreeProduct;

    /**
     * 搭销用来计算的模板： 模板id, 多个搭配id
     */
    private Map<String, List<Long>> templateForCombineId = new HashMap<>(2);

    /**
     * 搭销用来获取快递方式的模板： 搭配id，对应一个搭销模板
     */
    private Map<Long, String> combineIdForDeliveryTypeTemplate = new HashMap<>(2);

    /**
     * 搭销id对应可以计算的模板id： 搭销id, 多个模板id
     */
    private Map<Long, List<String>> combineIdForCalculateTemplate;

    /**
     * 通用模板
     */
    private PostageTemplateVo commonTemplate;

    /**
     * 特殊模板
     */
    private List<PostageTemplateVo> specialTemplate;

    private boolean isCommonTemplateAllowFree;

    private boolean isContainCombine;

    private  Map<Integer, String> itemProductMap;

    private  Map<Long, String> combineProductNameMap;

    private String platform;

    private String postageTip;

    private Integer payType;

    private List<ShopCartItem> shopCartItems;

    private List<PostageTemplateVo> allTemplateVos;

    public CalculateDirector(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String platform, Integer payType) {
        this.platform = platform;
        this.payType = payType;
        this.allTemplateVos = templateVos.stream().filter(t -> t.getPlatforms().contains(platform)).collect(Collectors.toList());
        this.shopCartItems = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).filter(Objects::nonNull).collect(Collectors.toList());
        this.shopCartProductCodes = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).filter(item -> item.getCombineId() == null).map(ShopCartItem::getProductCode).distinct().collect(Collectors.toList());
        this.shopCartCombineProductCodes = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).filter(item -> item.getCombineId() != null).map(ShopCartItem::getProductCode).distinct().collect(Collectors.toList());
        this.shopCartSkuCodes = shopCartProductCodes.stream().map(Long::intValue).distinct().collect(Collectors.toList());
        this.commonTemplate = allTemplateVos.stream().filter(t -> t.getType() == 0).findFirst().orElse(new PostageTemplateVo());
        this.specialTemplate = allTemplateVos.stream().filter(t -> t.getType() == 1).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(specialTemplate)) {
            List<Long> shopCartAllProductCodes = new ArrayList<>(shopCartCombineProductCodes);
            shopCartAllProductCodes.addAll(shopCartProductCodes);
            this.specialTemplateCalculateProduct = specialTemplate.stream().flatMap(t -> t.getProductCodes().stream()).map(Integer::longValue).filter(shopCartAllProductCodes::contains).collect(Collectors.toList());
            log.info("在特殊模板配置的商品(同时购物车有的)：{}", specialTemplateCalculateProduct);
            this.unFreeProduct = unFreeProduct(specialTemplate, payType, platform);
            log.info("特殊模板不能参与免邮计算的商品：{}", unFreeProduct);
        }

        this.commonTemplateCalculateProduct = shopCartProductCodes.stream().filter(sku -> !specialTemplateCalculateProduct.contains(sku)).collect(Collectors.toList());
        log.info("可以在通用模板计算的商品：{}", commonTemplateCalculateProduct);

        this.isCommonTemplateAllowFree = commonTemplate.getPostageTypes().stream().filter(pt -> pt.getPayType().equals(payType)).anyMatch(pt -> pt.getIsAllowFree() == 1);
        log.info("通用模板是否允许包邮：{}", isCommonTemplateAllowFree);

        this.isContainCombine = isContainCombine(shopCartBase);
        log.info("购物车是否包含搭销商品：{}", isContainCombine);
        if (isContainCombine) {
            this.combineIdForCalculateTemplate = combineIdForCalculateTemplate(templateVos, shopCartBase, platform);
            this.templateForCombineId = templateCalculateCombineId(combineIdForCalculateTemplate);
            //this.templateForCombineId = templateCalculateCombineId(templateVos, shopCartBase, platform);
            combineProductNameMap = shopCartItems.stream().filter(item -> item.getCombineId() != null).collect(Collectors.toMap(ShopCartItem::getCombineId, ShopCartItem::getCombineName, (i, j) -> j));
            log.info("用来计算搭销的模板Id, 对应的搭销ids：{}", JSON.toJSONString(templateForCombineId));
        }
        this.itemProductMap = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).collect(Collectors.toMap(item -> item.getProductCode().intValue(), ShopCartItem::getProductName, (i, j) -> i));
    }

    private boolean isContainCombine(ShopCartBase shopCartBase) {
        return shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).anyMatch(item -> item.getCombineId() != null);
    }

    private List<Long> specialTemplateProduct(List<PostageTemplateVo> templateVos, String p, List<Long> shopCartProductCodes) {
        return templateVos.stream()
                .filter(t -> t.getType() == 1)
                .filter(t -> t.getPlatforms().contains(p))
                .flatMap(t -> t.getProductCodes().stream())
                .map(Integer::longValue)
                .filter(shopCartProductCodes::contains)
                .collect(Collectors.toList());
    }

    //特殊模板，不能参与免邮计算的商品
    private List<Long> unFreeProduct(List<PostageTemplateVo> templateVos, Integer payType, String p) {
        return templateVos.stream()
                .filter(t -> t.getPostageTypes().stream()
                        .filter(pt -> payType.equals(pt.getPayType()))
                        .anyMatch(pt -> pt.getIsAllowFree() == 0))
                .flatMap(t -> t.getProductCodes().stream())
                .map(Integer::longValue)
                .collect(Collectors.toList());
    }

    private Map<Long, List<String>> combineIdForCalculateTemplate(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p) {
        Map<Long, List<String>> combineIdForTemplateIdMap = new HashMap<>();
        //先找出结算商品中所有的搭配(combineMap: 搭配Id, 搭配Id对应的商品编码)
        Map<Long, Set<Long>> combineMap = shopCartBase.getMerchants().stream()
                .flatMap(m -> m.getItems().stream())
                .filter(cartItem -> cartItem.getCombineId() != null)
                .collect(Collectors.groupingBy(ShopCartItem::getCombineId, Collectors.collectingAndThen(
                        toSet(), item -> item.stream().map(ShopCartItem::getProductCode).collect(toSet()))));

        for (Long combineId : combineMap.keySet()) {
            List<String> templateIds = new ArrayList<>();
            //搭销的商品，不在任意的特殊模板当中， 则此搭销可以在通用模板(允许包邮)计算，以及所有的允许包邮的特殊模板
            if (combineMap.get(combineId).stream().noneMatch(sku -> specialTemplateCalculateProduct.contains(sku))) {
                if (commonTemplate != null && isCommonTemplateAllowFree) {
                    templateIds.add(commonTemplate.getId());
                }
                combineIdForDeliveryTypeTemplate.put(combineId, commonTemplate.getId());
                if (CollectionUtils.isNotEmpty(specialTemplate)) {
                    List<String> ids = specialTemplate.stream().filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                            .map(PostageTemplateVo::getId).collect(Collectors.toList());
                    templateIds.addAll(ids);
                }
            } else {
                PostageTemplateVo specialTemplateMax = specialTemplate.stream()
                        .filter(t -> t.getProductCodes() != null && combineMap.get(combineId).stream().anyMatch(sku -> t.getProductCodes().contains(sku.intValue())))
                        .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                        .max(Comparator.comparing(PostageTemplateVo::getFreePostagePrice)).orElse(null);
                //搭销在特殊模板(允许包邮)当中
                if (specialTemplateMax != null) {
                    templateIds = specialTemplate.stream().filter(t -> t.getFreePostagePrice() >= specialTemplateMax.getFreePostagePrice())
                            .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                            .map(PostageTemplateVo::getId).collect(Collectors.toList());
                    combineIdForDeliveryTypeTemplate.put(combineId, specialTemplateMax.getId());
                } else {
                    //极端情况：搭销在特殊模板当中，但是没有找到包邮的特殊模板，只能用门槛最高的特殊门槛
                    PostageTemplateVo unFreeSpecialTemplateMax = specialTemplate.stream()
                            .filter(t -> t.getProductCodes() != null && combineMap.get(combineId).stream().anyMatch(sku -> t.getProductCodes().contains(sku.intValue())))
                            .max(Comparator.comparing(PostageTemplateVo::getFreePostagePrice)).orElse(null);
                    templateIds.add(unFreeSpecialTemplateMax == null ? commonTemplate.getId() : unFreeSpecialTemplateMax.getId());
                    combineIdForDeliveryTypeTemplate.put(combineId, unFreeSpecialTemplateMax.getId());
                }
            }
            combineIdForTemplateIdMap.put(combineId, templateIds);
        }
        log.info("搭销id对应可以计算的模板id： 搭销id, 多个模板id====" + JSON.toJSONString(combineIdForTemplateIdMap));
        return combineIdForTemplateIdMap;
    }


    /**
     * 搭销id对应可以计算的模板id： 搭销id, 多个模板id
     * 转换为： 模板id, 多个搭配id
     * @param map
     * @return
     */
    private Map<String, List<Long>> templateCalculateCombineId(Map<Long, List<String>> map) {
        //模板id, 多个搭配id
        Map<String, List<Long>> templateForCombineMap = new HashMap<>();
        Set<String> templateIds = map.keySet().stream().flatMap(combineId -> map.get(combineId).stream()).filter(Objects::nonNull).collect(Collectors.toSet());
        for (String id : templateIds) {
            List<Long> combineIds = map.keySet().stream().filter(combineId -> map.get(combineId).contains(id)).collect(Collectors.toList());
            templateForCombineMap.put(id, combineIds);
        }
        return templateForCombineMap;
    }


    private Map<String, List<Long>> templateCalculateCombineId(List<PostageTemplateVo> templateVos, ShopCartBase shopCartBase, String p) {
        //先找出结算商品中所有的搭配(combineMap: 搭配Id, 搭配Id对应的商品编码)
        Map<Long, Set<Integer>> combineMap = shopCartBase.getMerchants().stream()
                .flatMap(m -> m.getItems().stream())
                .filter(cartItem -> cartItem.getCombineId() != null)
                .collect(Collectors.groupingBy(ShopCartItem::getCombineId, Collectors.collectingAndThen(
                        toSet(), item -> item.stream().map(i -> i.getProductCode().intValue()).collect(toSet()))));

        String commonTemplateId = commonTemplate != null ? commonTemplate.getId() : "";

        //模板id, 多个搭配id
        Map<String, List<Long>> templateForCombineMap = new HashMap<>();
        //一个个搭配来处理，找到包邮的特殊模板中，门槛最高的那个， 找不到就用通用模板
        for (Long combineId : combineMap.keySet()) {
            //先在特殊模板(允许包邮的)包含搭配商品的模板中，找到门槛最高的
            PostageTemplateVo specialTemplateMax = templateVos.stream().filter(t -> t.getType() == 1 && t.getPlatforms().contains(p))
                    .filter(t -> t.getProductCodes() != null && combineMap.get(combineId).stream().anyMatch(t.getProductCodes()::contains))
                    .filter(t -> t.getPostageTypes().stream().anyMatch(pt -> CollectionUtils.isNotEmpty(pt.getFreeDeliveryTypeVos())))
                    .max(Comparator.comparing(PostageTemplateVo::getFreePostagePrice)).orElse(null);

            String calculateTemplateId = specialTemplateMax != null ? specialTemplateMax.getId() : commonTemplateId;
            templateForCombineMap.computeIfAbsent(calculateTemplateId, k -> new ArrayList<>());
            templateForCombineMap.get(calculateTemplateId).add(combineId);
        }
        return templateForCombineMap;
    }
}
