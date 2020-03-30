package com.jianke.vo;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Getter
@Setter
public class PostageTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 配置id
     */
    private String id;
    /**
     * 即将生效的配置id
     */
    private String relativeId;
    /**
     * 配置类别  0:通用 1特殊
     */
    private Integer type;
    /**
     * 生效平台
     */
    private List<String> platforms;
    /**
     * 配置状态 0失效，1有效
     */
    private Integer status;

    /**
     * 配置状态名称
     */
    private String statusName;

    /**
     * 配置名称
     */
    private String templateName;

    /**
     * 包邮金额
     */
    private Long freePostagePrice;

    /**
     * 快递方式
     */
    private List<PostageTypeVo> postageTypes;

    /**
     * 商品id
     */
    private List<Integer> productCodes;

    private List<PostageProduct> products;
    /**
     * 生效开始时间
     */
    private Date beginDate;
    
    private Date createdDate;

    private Date lastModifiedDate;

    private String createdBy;

    private String lastModifiedBy;

    private Integer recordVersion;

    /**
     * 删除标识
     */
    private Integer deleteFlag;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostageProduct {
        private Integer productCode;
        private String productName;

    }

    public void addPostageType(PostageTypeVo vo) {
        if (postageTypes == null) {
            postageTypes = new ArrayList<>();
        }
        postageTypes.add(vo);
    }
}
