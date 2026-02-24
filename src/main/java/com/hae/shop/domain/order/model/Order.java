package com.hae.shop.domain.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Order {

    private Long id;
    private Long memberId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal paymentAmount;
    private OrderStatus status = OrderStatus.PENDING;
    private String idempotencyKey;
    private List<OrderItem> items = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public enum OrderStatus {
        PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getOrderNumber() { return orderNumber; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPaymentAmount() { return paymentAmount; }
    public OrderStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
