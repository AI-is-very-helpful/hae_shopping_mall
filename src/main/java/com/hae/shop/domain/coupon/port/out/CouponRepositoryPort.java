package com.hae.shop.domain.coupon.port.out;

import com.hae.shop.domain.coupon.model.Coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepositoryPort {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findActiveCoupons();

    List<Coupon> findByMemberId(Long memberId);
}
