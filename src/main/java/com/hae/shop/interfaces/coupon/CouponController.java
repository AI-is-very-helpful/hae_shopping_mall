package com.hae.shop.interfaces.coupon;

import com.hae.shop.domain.coupon.model.Coupon;
import com.hae.shop.domain.coupon.port.in.CouponService;
import com.hae.shop.interfaces.coupon.dto.CreateCouponRequest;
import com.hae.shop.interfaces.coupon.dto.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = new Coupon();
        coupon.setCode(request.code());
        coupon.setName(request.name());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.type()));
        coupon.setDiscountValue(request.discountValue());
        coupon.setMinPurchaseAmount(request.minPurchaseAmount());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setValidFrom(request.validFrom());
        coupon.setValidUntil(request.validTo());
        coupon.setTotalQuantity(request.totalQuantity());
        coupon.setRemainingQuantity(request.totalQuantity());
        coupon.setStatus(Coupon.CouponStatus.ACTIVE);
        
        Coupon created = couponService.createCoupon(coupon);
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.getCoupon(id);
        return ResponseEntity.ok(CouponResponse.from(coupon));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CouponResponse>> getActiveCoupons() {
        List<Coupon> coupons = couponService.getActiveCoupons();
        List<CouponResponse> responses = coupons.stream()
            .map(CouponResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
