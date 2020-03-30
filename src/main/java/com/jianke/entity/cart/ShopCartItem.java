package com.jianke.entity.cart;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 购物车商品项
 * @author rongliangkang
 * @create 2017-06-09 17:47
 **/
@Setter
@Getter
@ToString
@Accessors(chain = true)
@NoArgsConstructor
public class ShopCartItem implements Serializable{
    /**
     * 商品项ID,是一个UUID
     */
    private String id;
    /**
     * 搭配ID
     */
    private Long combineId;
    /**
     * 搭配名称
     */
    private String combineName;
    /**
     * 产品编码
     */
    private Long productCode;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 产品图片，小图
     */
    private String productPic;
    /**
     * 规格
     */
    private String packing;
    /**
     * 健客价
     */
    private Long ourPrice;
    /**
     * 市场价
     */
    private Long marketPrice;
    /**
     * 实际支付价
     */
    private Long actualPrice;
    /**
     * 搭配数量
     */
    private int combineNum;

    private String usingCouponMark;

    private Long combinePrice;

    private Long combineTotalAmount;

    private Integer combineStatus;
    /**
     * 购买数量
     */
    private int productNum;
    /**
     * 状态
     */
    private Integer itemStatus;
    /**
     * 是否选中
     */
    private String isSelected;
    /**
     * 税费（全球购）
     */
    private String gTax;
    /**
     * 税率（全球购）
     */
    private String gTaxRate;
    /**
     * 类型
     */
    private Integer type;
    /**
     * 来源
     */
    private String sourceHost;

    /**
     * 是否是处方药，0 :(否) ,1 :(是)
     */
    private String isRx;
    /**
     * 是否是药品
     */
    private Boolean isDrug;
    /**
     * 是否全球购商品
     */
    private Boolean isGlobal;
    /**
     * 是否长安医院药品
     */
    private Boolean isChangAnDrug;

    // 加入购物车时的信息
    /**
     * 添加到购物车的时间
     */
    private Date addDate;
    private Integer addNum;
    private Integer addPlatformId;
    private Long addPrice;


    private Integer inventoryNum = 100;

    private String inventoryTip;

    public ShopCartItem(String i) {
        String[] param = i.split("-");
        this.productCode = Long.valueOf(param[0]);
        this.productName = param[1];
        this.productNum = Integer.valueOf(param[2]);
        this.actualPrice = Long.valueOf(param[3]);
        this.isRx = param[4];
    }

    public List<ShopCartItem> combine(String i, String j, Integer id) {
        String[] param = i.split("-");
        String[] param1 = j.split("-");
        ShopCartItem item = new ShopCartItem()
                .setCombineId(id.longValue())
                .setProductCode(Long.valueOf(param[0]))
                .setProductName(param[1])
                .setCombineNum(Integer.valueOf(param[2]))
                .setActualPrice(Long.valueOf(param[3]))
                .setIsRx(param[4]);
        ShopCartItem item1 = new ShopCartItem()
                .setCombineId(id.longValue())
                .setProductCode(Long.valueOf(param1[0]))
                .setProductName(param1[1])
                .setCombineNum(Integer.valueOf(param1[2]))
                .setActualPrice(Long.valueOf(param1[3]))
                .setIsRx(param1[4]);
        return Arrays.asList(item, item1);
    }
}
