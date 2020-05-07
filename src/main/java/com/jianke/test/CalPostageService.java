package com.jianke.test;

import com.alibaba.fastjson.JSON;
import com.jianke.entity.CouponParam;
import com.jianke.entity.cart.MerchantItem;
import com.jianke.entity.cart.SettlementItem;
import com.jianke.entity.cart.SettlementProduct;
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
        templateVo.addType(new PostageTypeVo("1","7-顺丰-0|11-圆通-0", "5-EMS-8|11-圆通-10"));
//        templateVo.addType(new PostageTypeVo("0","5-EMS-18"));
        log.info(templateVo.getTemplateName() + "------" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate2() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(100 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("16,17特殊模板100包邮");
        templateVo.setProductCodes(Arrays.asList(15,16,17));
        templateVo.setId("specialTemplate2");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
        templateVo.addType(new PostageTypeVo("1", "11-圆通-0|5-EMS-0", "11-圆通-8|5-EMS-15"));
//        templateVo.addType(new PostageTypeVo("0", "11-圆通-8|5-EMS-15"));
        log.info(templateVo.getTemplateName() + "-----\n" + JSON.toJSONString(templateVo));
        return templateVo;
    }

    public static PostageTemplateVo specialTemplate3() {
        //设置特殊运费模板
        PostageTemplateVo templateVo = new PostageTemplateVo();
        templateVo.setFreePostagePrice(60 * 100L);
        templateVo.setPlatforms(Arrays.asList("app")).setStatus(1);
        templateVo.setType(1).setTemplateName("18特殊模板60包邮");
        templateVo.setProductCodes(Arrays.asList(18,19,20,21));
        templateVo.setId("specialTemplate3");
        //设置快递方式  是否支持免邮(1是/0否),  免邮快递方式， 不免邮快递方式
//        templateVo.addType(new PostageTypeVo("1", "7-顺丰-0|11-圆通-0", "7-顺丰-8|5-EMS-6"));
        templateVo.addType(new PostageTypeVo("0", "5-EMS-5"));
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
        List<PostageTemplateVo> templateVos = Arrays.asList(commonTemplate1(),specialTemplate2(),specialTemplate3(),specialTemplate4());
        SettlementItem settlementItem = buildShopCartBase();
        String platform = "app";
        List<Long> freePostage = Arrays.asList(80L,81L,82L,83L,84L,85L);
//        getPostageLabel(templateVos, 8, platform);
        //2是全场券， 3是商品券
//        CouponParam coupon = new CouponParam(2, "全场券", 1000, null);
        CouponParam coupon = new CouponParam(3, "商品券003", 2000, Arrays.asList(31L,2L,3L));
        CouponParam coupon3 = new CouponParam(3, "商品券004", 3000, Arrays.asList(11L,5L,16L));
        List<CouponParam> couponParams = Arrays.asList(coupon, coupon3);
        PostageCalculateAlgorithm.startPostageCalculate(templateVos, settlementItem, platform, 99, freePostage, couponParams);
    }


    public static SettlementItem buildShopCartBase() {
        MerchantItem merchantItem = new MerchantItem();
        SettlementItem shop = new SettlementItem(Arrays.asList(merchantItem));
        merchantItem.setMerchantCode(1).setMerchantName("健客自营");
        //编码-名称-数量-单个商品价格(分)
        List<SettlementProduct> list = new ArrayList<>();
//        list.add(new SettlementProduct("85-商品名称11-3-2000"));
//        list.add(new SettlementProduct("17-商品名称12-1-1000"));
        list.add(new SettlementProduct("16-商品名称22-1-2000"));
        list.add(new SettlementProduct("18-商品名称18-1-1000"));
        list.add(new SettlementProduct("31-商品名称31-1-3000"));
        list.add(new SettlementProduct("11-商品名称1-1-6000"));
//        list.addAll(new SettlementProduct().combine("2-商品31-1-1-1000","5-商品31-1-1-1000","16-商品31-1-1-1000", 100001, "感冒搭销装"));
//        list.addAll(new SettlementProduct().combine("2-商品31-1-1-1000","9-商品31-1-1-1000","18-商品31-1-1-500", 100002, "犯困搭销装"));
        merchantItem.setProductList(list);
        log.info("购物车商品------\n" + JSON.toJSONString(shop) + "\n");
        return shop;
    }
}
