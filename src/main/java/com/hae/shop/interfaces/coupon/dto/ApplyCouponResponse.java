package com.hae.shop.interfaces.coupon.dto;
import java.math.BigDecimal;


/**
 * 쿠폰 적용 응답 DTO (할인 금액 반환)
 */
public record ApplyCouponResponse(
    Long couponId,
    String couponName,
    BigDecimal discountAmount,
    BigDecimal finalAmount
) {
}
