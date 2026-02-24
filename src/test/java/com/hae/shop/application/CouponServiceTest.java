package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.coupon.model.Coupon;
import com.hae.shop.domain.coupon.port.out.CouponRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepositoryPort couponRepository;

    @InjectMocks
    private CouponServiceImpl couponService;

    private Coupon testCoupon;
    private Instant futureDate;
    private Instant pastDate;

    @BeforeEach
    void setUp() {
        futureDate = Instant.now().plus(30, ChronoUnit.DAYS);
        pastDate = Instant.now().minus(30, ChronoUnit.DAYS);

        testCoupon = new Coupon();
        testCoupon.setId(1L);
        testCoupon.setCode("SAVE10");
        testCoupon.setName("10% Discount");
        testCoupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        testCoupon.setDiscountValue(BigDecimal.valueOf(10));
        testCoupon.setMinPurchaseAmount(BigDecimal.valueOf(10000));
        testCoupon.setMaxDiscountAmount(BigDecimal.valueOf(5000));
        testCoupon.setValidFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        testCoupon.setValidUntil(futureDate);
        testCoupon.setTotalQuantity(100);
        testCoupon.setRemainingQuantity(50);
        testCoupon.setStatus(Coupon.CouponStatus.ACTIVE);
    }

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_shouldReturnCoupon() {
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        Coupon result = couponService.createCoupon(testCoupon);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("SAVE10");
        verify(couponRepository).save(testCoupon);
    }

    @Test
    @DisplayName("쿠폰 조회 성공")
    void getCoupon_whenExists_shouldReturnCoupon() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        Coupon result = couponService.getCoupon(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("SAVE10");
    }

    @Test
    @DisplayName("쿠폰 조회 실패 - 존재하지 않음")
    void getCoupon_whenNotExists_shouldThrowException() {
        when(couponRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCoupon(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_FOUND));
    }

    @Test
    @DisplayName("활성 쿠폰 목록 조회")
    void getActiveCoupons_shouldReturnActiveCoupons() {
        when(couponRepository.findActiveCoupons()).thenReturn(List.of(testCoupon));

        List<Coupon> result = couponService.getActiveCoupons();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("SAVE10");
    }

    @Test
    @DisplayName("정률 할인 계산 - 정상")
    void applyDiscount_percentageType_shouldCalculateCorrectly() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        BigDecimal result = couponService.applyDiscount(1L, BigDecimal.valueOf(20000));

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    @DisplayName("정률 할인 계산 - 최대 할인 금액 한도 적용")
    void applyDiscount_shouldCapAtMaxDiscountAmount() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        BigDecimal result = couponService.applyDiscount(1L, BigDecimal.valueOf(100000));

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("정액 할인 계산")
    void applyDiscount_fixedType_shouldCalculateCorrectly() {
        testCoupon.setDiscountType(Coupon.DiscountType.FIXED);
        testCoupon.setDiscountValue(BigDecimal.valueOf(5000));
        testCoupon.setMaxDiscountAmount(null);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        BigDecimal result = couponService.applyDiscount(1L, BigDecimal.valueOf(20000));

        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(5000));
    }

    @Test
    @DisplayName("할인 적용 실패 - 최소 구매 금액 미달")
    void applyDiscount_whenBelowMinPurchase_shouldReturnZero() {
        testCoupon.setMinPurchaseAmount(BigDecimal.valueOf(50000));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        BigDecimal result = couponService.applyDiscount(1L, BigDecimal.valueOf(20000));

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("할인 적용 실패 - 쿠폰 유효하지 않음 (만료)")
    void applyDiscount_whenCouponExpired_shouldThrowException() {
        testCoupon.setValidUntil(pastDate);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        assertThatThrownBy(() -> couponService.applyDiscount(1L, BigDecimal.valueOf(20000)))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("할인 적용 실패 - 쿠폰 비활성화")
    void applyDiscount_whenCouponInactive_shouldThrowException() {
        testCoupon.setStatus(Coupon.CouponStatus.EXPIRED);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        assertThatThrownBy(() -> couponService.applyDiscount(1L, BigDecimal.valueOf(20000)))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_NOT_AVAILABLE));
    }

    @Test
    @DisplayName("쿠폰 사용 - 성공")
    void useCoupon_shouldDecrementRemainingQuantity() {
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        couponService.useCoupon(1L, 1L);

        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 수량 소진")
    void useCoupon_whenQuotaExceeded_shouldThrowException() {
        testCoupon.setRemainingQuantity(0);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(testCoupon));

        assertThatThrownBy(() -> couponService.useCoupon(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.COUPON_QUOTA_EXCEEDED));
    }
}
