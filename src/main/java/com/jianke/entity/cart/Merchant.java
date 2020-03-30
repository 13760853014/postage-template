package com.jianke.entity.cart;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 商家信息
 * @author rongliangkang
 * @create 2017-06-09 17:42
 **/
@Getter
@Setter
@ToString
@NoArgsConstructor
@Accessors(chain = true)
public class Merchant extends MerchantBase {

    /**
     * 商品项列表
     */
    private List<ShopCartItem> items;
    
    private String channelId;
    
}
