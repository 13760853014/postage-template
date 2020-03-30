package com.jianke.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 商城活动优惠券
 *
 * @author huhua
 * @since 2019/3/8
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
public class Coupon implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long couponId;

	private String couponName;

	private Long couponValue;

	private Long minConsume;

	private Integer type;

	private Integer amount;

	private Date startDrawDate;

	private Date endDrawDate;

	private Byte relativeFlag;

	private Integer relativeDays;

	List<Long> productCodes;

	public Coupon(String i) {
		String[] param = i.split("-");
		this.couponValue = Long.valueOf(param[0]);
		this.type = Integer.valueOf(param[1]);
		if (type == 2) {
			this.productCodes = Stream.of(param[2].split(",")).map(s -> Long.valueOf(s)).collect(Collectors.toList());
		}
	}

}
