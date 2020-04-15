package com.jianke.test;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.Coupon;
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

public class CalPostageService {
    private static final Logger log = LoggerFactory.getLogger(CalPostageService.class);

    public static PostageTemplateVo commonTemplate1() {
        //设置通用运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(99 * 100L);
        templateVo.setPlatforms(Arrays.asList("app","mobile","mini")).setStatus(1);
        templateVo.setType(0).setTemplateName("通用模板");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|5-EMS-0", "7-顺丰-10|5-EMS-12"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate2() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(15 * 100L);
        templateVo.setPlatforms(Arrays.asList("app","mobile","mini")).setStatus(1);
        templateVo.setType(1).setTemplateName("特殊模板2222");
        templateVo.setProductCodes(Arrays.asList(21,22,23,24,25,26,27,28,29));
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|5-EMS-0", "7-顺丰-10|5-EMS-12"));
        log.info(templateVo.getTemplateName() + "-----" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate3() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(20 * 100L);
        templateVo.setPlatforms(Arrays.asList("app","mobile","mini")).setStatus(1);
        templateVo.setType(1).setTemplateName("特殊模板333");
        templateVo.setProductCodes(Arrays.asList(31,32,33,34,35,36,37,38,39));
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("0", "7-顺丰-10|5-EMS-12"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static void main(String[] args) {
        List<PostageTemplateVo> templateVos = Arrays.asList(commonTemplate1(), specialTemplate2(), specialTemplate3());
        ShopCartBase shopCartBase = buildShopCartBase();
        //优惠券金额-类型-商品（1全场券，2商品券， 商品用,隔开）
        List<Coupon> coupons = Arrays.asList(new Coupon("1000-1"), new Coupon("1000-2-11,12"));
        String platform = "app";
        List<Long> freePostage = Arrays.asList(80L,81L,82L,83L,84L,85L);
        List<Long> coldChainSku = Arrays.asList(90L,91L,92L,93L,94L,95L);
        //判断商品是否免邮，已经获取对应的运费类型
        boolean isFree = PostageAlgorithm.calPostageIsFree(templateVos, shopCartBase, platform, 99, coupons, freePostage, coldChainSku);
        List<DeliveryTypeVo> deliveryTypeVos = PostageAlgorithm.getPostageType(templateVos, shopCartBase, platform, 99, isFree, coldChainSku);
        log.info("【最终结果】：  平台{}，支付方式{}，是否包邮[{}]，返回的快递方式:\n{}\n", platform, 99, isFree, JSON.toJSONString(deliveryTypeVos));

        //商品在详情页展示的邮费标签
        PostageAlgorithm.getPostageLabel(templateVos, 99, platform);
    }

    public static ShopCartBase buildShopCartBase() {
        Merchant merchant = new Merchant();
        ShopCartBase shop = new ShopCartBase(Arrays.asList(merchant));
        merchant.setMerchantCode(1).setMerchantName("健客自营");
        //编码-名称-数量-单个商品价格(分)
        List<ShopCartItem> list = new ArrayList<>();
        list.add(new ShopCartItem("11-商品名称11-3-3000"));
//        list.add(new ShopCartItem("12-商品名称12-5-4000"));
//        list.add(new ShopCartItem("22-商品名称22-3-3000"));
//        list.add(new ShopCartItem("23-商品名称23-3-3000"));
//        list.add(new ShopCartItem("31-商品名称31-3-3000"));
        //list.addAll(new ShopCartItem().combine("31-商品31-3-3000","31-商品31-3-3000", 100001));
        merchant.setItems(list);
        log.info("购物车商品------" + JSON.toJSONString(shop) + "\n");
        return shop;
    }
}
