package com.hae.shop.domain.coupon.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Coupon {

    private Long id;
    private String code;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minPurchaseAmount;
    private BigDecimal maxDiscountAmount;
    private Instant validFrom;
    private Instant validUntil;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private CouponStatus status = CouponStatus.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;

    public enum DiscountType {
        FIXED, PERCENTAGE
    }

    public enum CouponStatus {
        ACTIVE, EXPIRED, DEPLETED
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount.compareTo(minPurchaseAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (discountType) {
            case FIXED -> discountValue;
            case PERCENTAGE -> orderAmount.multiply(discountValue).divide(BigDecimal.valueOf(100));
        };

        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }

        return discount;
    }

    public boolean isValid() {
        Instant now = Instant.now();
        boolean withinPeriod = !now.isBefore(validFrom) && !now.isAfter(validUntil);
        boolean hasRemaining = remainingQuantity == null || remainingQuantity > 0;
        return withinPeriod && hasRemaining && status == CouponStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMinPurchaseAmount() { return minPurchaseAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public Integer getTotalQuantity() { return totalQuantity; }
    public Integer getRemainingQuantity() { return remainingQuantity; }
    public CouponStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setName(String name) { this.name = name; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public void setMinPurchaseAmount(BigDecimal minPurchaseAmount) { this.minPurchaseAmount = minPurchaseAmount; }
    public void setMaxDiscountAmount(BigDecimal maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public void setValidUntil(Instant validUntil) { this.validUntil = validUntil; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }
    public void setRemainingQuantity(Integer remainingQuantity) { this.remainingQuantity = remainingQuantity; }
    public void setStatus(CouponStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
