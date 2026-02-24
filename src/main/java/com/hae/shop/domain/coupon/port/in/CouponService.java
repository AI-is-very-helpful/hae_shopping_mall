package com.hae.shop.domain.coupon.port.in;

import com.hae.shop.domain.coupon.model.Coupon;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    Coupon createCoupon(Coupon coupon);

    Coupon getCoupon(Long id);

    List<Coupon> getActiveCoupons();

    BigDecimal applyDiscount(Long couponId, BigDecimal orderAmount);

    void useCoupon(Long couponId, Long memberId);
}
