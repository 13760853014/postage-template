package com.jianke.entity;

import lombok.Data;

import java.util.List;

/**
 * 运费模板快递配置
 * @author shichenru
 * @since 2020-03-13
 */
@Data
public class PostageType {

    /**
     * 支付类型, 1货到付款， 99在线
     */
    private Integer payType;

    /**
     * 是否允许包邮
     */
    private Integer isAllowFree;

    /**
     * 允许包邮快递方式
     */
    private List<String> freeDeliveryTypeIds;

    /**
     * 不允许包邮快递方式
     */
    private List<String> unFreeDeliveryTypeIds;


}
