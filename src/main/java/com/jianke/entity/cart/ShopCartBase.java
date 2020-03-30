package com.jianke.entity.cart;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * @author rongliangkang
 * @create 2017-06-09 18:07
 **/
@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShopCartBase implements Serializable {
    /**
     * 商家列表
     */
    List<Merchant> merchants;

}
