package com.jianke.entity.cart;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 商家基本信息
 * @author rongliangkang
 * @create 2017-06-09 20:51
 **/
@Setter
@Getter
@ToString
@Accessors(chain = true)
public class MerchantBase implements Serializable{
    /**
     * 商家编码
     */
    private Integer merchantCode;
    /**
     * 商家名称
     */
    private String merchantName;
}
