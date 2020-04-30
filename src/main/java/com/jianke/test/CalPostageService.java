package com.jianke.test;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.cart.Merchant;
import com.jianke.entity.cart.ShopCartBase;
import com.jianke.entity.cart.ShopCartItem;
import com.jianke.service.PostageCalculateAlgorithm;
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
        templateVo.setFreePostagePrice(59 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(0).setTemplateName("允许包邮通用模板");
        templateVo.setId("commonTemplate1");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
//        templateVo.addType(new PostageTypeVo("1","7-顺丰-0|6-EMS-0", "5-EMS-28"));
        templateVo.addType(new PostageTypeVo("0","5-EMS-18"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate2() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(100 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("16,17特殊模板100包邮");
        templateVo.setProductCodes(Arrays.asList(16,17));
        templateVo.setId("specialTemplate2");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
//        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|11-圆通-0", "11-圆通-8|5-EMS-15"));
        templateVo.addType(new PostageTypeVo("0", "11-圆通-25"));
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
        templateVo.setId("specialTemplate3");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
//        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|11-圆通-0", "7-顺丰-18|5-EMS-20"));
        templateVo.addType(new PostageTypeVo("0", "5-EMS-15"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }
    public static PostageTemplateVo specialTemplate4() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(70 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("1特殊模板70包邮");
        templateVo.setProductCodes(Arrays.asList(1));
        templateVo.setId("specialTemplate4");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|11-圆通-0", "5-EMS-20"));
//        templateVo.addType(new PostageTypeVo("0", "6-申通-10|5-EMS-15"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static void main(String[] args) {
        List<String> postageTip = new ArrayList<>(2);
        List<PostageTemplateVo> templateVos = Arrays.asList(commonTemplate1(),specialTemplate2(),specialTemplate3());
        ShopCartBase shopCartBase = buildShopCartBase();
        String platform = "app";
        List<Long> freePostage = Arrays.asList(80L,81L,82L,83L,84L,85L);
        PostageCalculateAlgorithm.startPostageCalculate(templateVos, shopCartBase, platform, 99, freePostage);
    }


    public static ShopCartBase buildShopCartBase() {
        Merchant merchant = new Merchant();
        ShopCartBase shop = new ShopCartBase(Arrays.asList(merchant));
        merchant.setMerchantCode(1).setMerchantName("健客自营");
        //编码-名称-数量-单个商品价格(分)
        List<ShopCartItem> list = new ArrayList<>();
//        list.add(new ShopCartItem("85-商品名称11-3-2000"));
//        list.add(new ShopCartItem("17-商品名称12-1-1000"));
//        list.add(new ShopCartItem("16-商品名称22-1-2000"));
//        list.add(new ShopCartItem("18-商品名称18-6-1000"));
//        list.add(new ShopCartItem("31-商品名称31-1-3000"));
//        list.add(new ShopCartItem("1-商品名称1-1-3000"));
        list.addAll(new ShopCartItem().combine("12-商品31-1-1-10000","19-商品31-1-1-3000", 100001, "感冒搭销装"));
        merchant.setItems(list);
        log.info("购物车商品------\n" + JSON.toJSONString(shop) + "\n");
        return shop;
    }
}
