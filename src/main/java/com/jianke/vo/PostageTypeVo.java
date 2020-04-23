package com.jianke.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@Slf4j
public class PostageTypeVo {

    /**
     * 支付类型, 1货到付款， 99在线
     */
    private Integer payType;

    /**
     * 是否允许包邮 0:否 1：是
     */
    private Integer isAllowFree;

    /**
     * 允许包邮快递方式
     */
    private List<String> freeDeliveryTypeIds;

    /**
     * 允许包邮快递方式
     */
    private List<DeliveryTypeVo> freeDeliveryTypeVos;

    /**
     * 不允许包邮快递方式
     */
    private List<String> unFreeDeliveryTypeIds;

    /**
     * 不允许包邮快递方式
     */
    private List<DeliveryTypeVo> unFreeDeliveryTypeVos;

    public PostageTypeVo(String i, String free, String unfree) {
        String[] param = i.split("-");
        this.isAllowFree = Integer.valueOf(param[0]);
        this.payType = 99;
        if (isAllowFree == 0) {
            log.info("-----邮费模板配置错误-----");
        }
        String[] freeArr = free.split("\\|");
        String[] unFreeArr = unfree.split("\\|");
        if (freeArr.length == 2) {
            this.freeDeliveryTypeVos = new DeliveryTypeVo().build(freeArr[0], freeArr[1]);
        } else {
            this.freeDeliveryTypeVos = Arrays.asList(new DeliveryTypeVo(freeArr[0]));
        }

        if (unFreeArr.length == 2) {
            this.unFreeDeliveryTypeVos = new DeliveryTypeVo().build(unFreeArr[0], unFreeArr[1]);
        } else {
            this.unFreeDeliveryTypeVos = Arrays.asList(new DeliveryTypeVo(unFreeArr[0]));
        }
    }

    public PostageTypeVo(String i, String unfree) {
        String[] param = i.split("-");
        this.isAllowFree = Integer.valueOf(param[0]);
        this.payType = 99;
        if (isAllowFree == 1) {
            log.info("-----邮费模板配置错误-----");
        }
        String[] unFreeArr = unfree.split("\\|");
        if (unFreeArr.length == 2) {
            this.unFreeDeliveryTypeVos = new DeliveryTypeVo().build(unFreeArr[0], unFreeArr[1]);
        } else {
            this.unFreeDeliveryTypeVos = Arrays.asList(new DeliveryTypeVo(unFreeArr[0]));
        }
    }

}
