package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.coupon.model.Coupon;
import com.hae.shop.domain.coupon.port.in.CouponService;
import com.hae.shop.domain.coupon.port.out.CouponRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final String CACHE_NAME = "coupons";
    private static final String ACTIVE_COUPONS_KEY = "active";

    private final CouponRepositoryPort couponRepository;

    @Override
    @Transactional
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
    public Coupon getCoupon(Long id) {
        return couponRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "'" + ACTIVE_COUPONS_KEY + "'")
    public List<Coupon> getActiveCoupons() {
        return couponRepository.findActiveCoupons();
    }

    @Override
    @Transactional
    public BigDecimal applyDiscount(Long couponId, BigDecimal orderAmount) {
        Coupon coupon = getCoupon(couponId);
        
        if (!coupon.isValid()) {
            throw new BusinessException(ErrorCode.COUPON_NOT_AVAILABLE);
        }
        
        return coupon.calculateDiscount(orderAmount);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void useCoupon(Long couponId, Long memberId) {
        Coupon coupon = getCoupon(couponId);
        
        if (coupon.getRemainingQuantity() == null || coupon.getRemainingQuantity() <= 0) {
            throw new BusinessException(ErrorCode.COUPON_QUOTA_EXCEEDED);
        }
        
        coupon.setRemainingQuantity(coupon.getRemainingQuantity() - 1);
        couponRepository.save(coupon);
    }
}
