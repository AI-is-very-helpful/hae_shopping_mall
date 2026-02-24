package com.hae.shop.interfaces.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 쿠폰 생성 요청 DTO
 */
public record CreateCouponRequest(
    String code,
    String name,
    String type,
    BigDecimal discountValue,
    BigDecimal minPurchaseAmount,
    BigDecimal maxDiscountAmount,
    Integer totalQuantity,
    Instant validFrom,
    Instant validTo
) {
}
