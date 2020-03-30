package com.jianke.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 运费模板实体类
 * @author shichenru
 * @since 2020-03-13
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Getter
@Setter
public class PostageTemplate implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 配置id
     */
    @Id
    private String id;

    private String relativeId;
    /**
     * 配置类别 0:通用 1特殊
     */
    private Integer type;
    /**
     * 生效平台 mobile,wechat,app,mini,pc
     */
    private List<String> platforms;
    /**
     * 配置状态 0失效，1有效
     */
    private Integer status;
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
    private List<PostageType> postageTypes;

    /**
     * 商品id
     */
    private List<Integer> productCodes;

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
}
