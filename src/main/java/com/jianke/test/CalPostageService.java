package com.jianke.test;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.cart.Merchant;
import com.jianke.entity.cart.ShopCartBase;
import com.jianke.entity.cart.ShopCartItem;
import com.jianke.service.PostageAlgorithm;
import com.jianke.vo.DeliveryTypeVo;
import com.jianke.vo.PostageTemplateVo;
import com.jianke.vo.PostageTypeVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalPostageService {
    private static final Logger log = LoggerFactory.getLogger(CalPostageService.class);

    public static PostageTemplateVo commonTemplate1() {
        //设置通用运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(59 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(0).setTemplateName("允许包邮通用模板");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1","7-顺丰-0|6-EMS-0", "7-顺丰-9|5-EMS-18"));
//        templateVo.addType(new PostageTypeVo("0","7-顺丰-10|5-EMS-18"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate2() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(100 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("16,17特殊模板69包邮");
        templateVo.setProductCodes(Arrays.asList(16,17));
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|6-申通-0", "6-申通-8|5-EMS-15"));
//        templateVo.addType(new PostageTypeVo("0", "6-申通-8|5-EMS-15"));
        log.info(templateVo.getTemplateName() + "-----\n" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate3() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(60 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("18特殊模板60包邮");
        templateVo.setProductCodes(Arrays.asList(18));
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|11-圆通-0", "7-顺丰-18|5-EMS-20"));
//        templateVo.addType(new PostageTypeVo("0", "6-申通-8|5-EMS-15"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static void main(String[] args) {
        List<String> postageTip = new ArrayList<>(2);
        List<PostageTemplateVo> templateVos = Arrays.asList(commonTemplate1(),specialTemplate2(),specialTemplate3());
        ShopCartBase shopCartBase = buildShopCartBase();
        String platform = "app";
        List<Long> freePostage = Arrays.asList(80L,81L,82L,83L,84L,85L);
        //判断商品是否免邮，已经获取对应的运费类型
        boolean isFree = PostageAlgorithm.calPostageIsFree(templateVos, shopCartBase, platform, 99, freePostage, postageTip);
        List<DeliveryTypeVo> deliveryTypeVos = PostageAlgorithm.getPostageType(templateVos, shopCartBase, platform, 99, isFree);
        log.info("【最终结果】：  平台{}，是否包邮[{}]，返回的快递方式:\n{}\n", platform, isFree, JSON.toJSONString(deliveryTypeVos));

        if (isFree) {
            log.info("【包邮运费提示语】 {}", postageTip.isEmpty() ? "" : postageTip.get(0));
        } else {
            Map<Integer, String> itemProductMap = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).collect(Collectors.toMap(item -> item.getProductCode().intValue(), item -> item.getProductName(), (i, j) -> i));
            List<Integer> itemProductCode = shopCartBase.getMerchants().stream().flatMap(m -> m.getItems().stream()).map(item -> item.getProductCode().intValue()).distinct().collect(Collectors.toList());

            //不包邮的情况下，需要重新计算运费提示语
            String postageDesc = PostageAlgorithm.postageDesc(templateVos, itemProductMap, itemProductCode, platform, 99);
            log.info("【不包邮运费提示语】 {}", postageDesc);
            if (deliveryTypeVos.size() == 1) {
                log.info("【不包邮运费提示语】  根据您选择的支付方式（在线支付）和快递方式（{}）, 收取{}元运费", deliveryTypeVos.get(0).getLogisticsName(), deliveryTypeVos.get(0).getDeliveryPrice() / 100);
            } else {
                String deliveryTypeDesc = PostageAlgorithm.deliveryTypeDesc(templateVos, itemProductMap, itemProductCode, platform, 99, deliveryTypeVos.get(0));
                log.info("【不包邮运费提示语】 {}", deliveryTypeDesc);
            }
        }
        //商品在详情页展示的邮费标签
        PostageAlgorithm.getPostageLabel(templateVos, 80, platform);
    }


    public static ShopCartBase buildShopCartBase() {
        Merchant merchant = new Merchant();
        ShopCartBase shop = new ShopCartBase(Arrays.asList(merchant));
        merchant.setMerchantCode(1).setMerchantName("健客自营");
        //编码-名称-数量-单个商品价格(分)
        List<ShopCartItem> list = new ArrayList<>();
//        list.add(new ShopCartItem("85-商品名称11-3-2000"));
//        list.add(new ShopCartItem("17-商品名称12-10-1000"));
        list.add(new ShopCartItem("18-商品名称22-1-2000"));
//        list.add(new ShopCartItem("23-商品名称23-1-1000"));
//        list.add(new ShopCartItem("31-商品名称31-1-3000"));
        //list.addAll(new ShopCartItem().combine("31-商品31-3-3000","31-商品31-3-3000", 100001));
        merchant.setItems(list);
        log.info("购物车商品------\n" + JSON.toJSONString(shop) + "\n");
        return shop;
    }
}
