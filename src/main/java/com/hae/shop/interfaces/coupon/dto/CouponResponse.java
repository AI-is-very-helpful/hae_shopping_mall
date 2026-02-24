package com.hae.shop.interfaces.coupon.dto;

import com.hae.shop.domain.coupon.model.Coupon;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 쿠폰 응답 DTO
 */
public record CouponResponse(
    Long id,
    String code,
    String name,
    String type,
    BigDecimal discountValue,
    BigDecimal minPurchaseAmount,
    BigDecimal maxDiscountAmount,
    Integer remainingQuantity,
    Instant validFrom,
    Instant validTo,
    String status
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
            coupon.getId(),
            coupon.getCode(),
            coupon.getName(),
            coupon.getDiscountType().name(),
            coupon.getDiscountValue(),
            coupon.getMinPurchaseAmount(),
            coupon.getMaxDiscountAmount(),
            coupon.getRemainingQuantity(),
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            coupon.getStatus().name()
        );
    }
}
