package com.jianke.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @Author: scr
 * @Date: 2020/3/10 12:05
 */
@Setter
@Getter
@ToString
@NoArgsConstructor
public class DeliveryTypeVo {

    private String id;
    //物流公司名
    private String logisticsName;
    //快递公司编码
    private String logisticsCode;
    //快递公司健客编号
    private String logisticsNum;
    //快递费用
    private Long deliveryPrice;
    //是否包邮 true:包邮
    private Boolean isFree;
    private Date createdDate;

    private Date lastModifiedDate;

    private String createdName;

    private String lastModifiedName;

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && hashCode() == obj.hashCode();
    }


    public DeliveryTypeVo(String logisticsNum, String logisticsName, Boolean isFree, Long deliveryPrice){
        this.id = logisticsName + deliveryPrice;
        this.logisticsNum = logisticsNum;
        this.logisticsName = logisticsName;
        this.isFree = isFree;
        this.deliveryPrice = deliveryPrice;
    }

    public DeliveryTypeVo(String i){
        String[] param = i.split("-");
        this.logisticsNum = param[0];
        this.logisticsName = param[1];
        //this.isFree = param[2].equals("true");
        this.deliveryPrice = Long.valueOf(param[2]) * 100;
        this.id = logisticsName + deliveryPrice;
    }

    public List<DeliveryTypeVo> build(String i, String j){
        return Arrays.asList(new DeliveryTypeVo(i), new DeliveryTypeVo(j));
    }

}
