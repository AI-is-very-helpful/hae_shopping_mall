package com.hae.shop.infrastructure.persistence.order;

import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.model.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA 엔티티: Order 도메인 객체의 영구 저장 표현.
 * 도메인 순수성을 위해基础设施 계층에 위치하며, 도메인 의존성은 없습니다.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal discountAmount = java.math.BigDecimal.ZERO;

    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal paymentAmount;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Order.OrderStatus status = Order.OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * 도메인 Order에서 OrderEntity로 변환 (持久化 전용).
     */
    public static OrderEntity fromDomain(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.getId();
        entity.memberId = order.getMemberId();
        entity.orderNumber = order.getOrderNumber();
        entity.totalAmount = order.getTotalAmount();
        entity.discountAmount = order.getDiscountAmount();
        entity.paymentAmount = order.getPaymentAmount();
        entity.idempotencyKey = order.getIdempotencyKey();
        entity.status = order.getStatus();
        entity.createdAt = order.getCreatedAt();
        entity.updatedAt = order.getUpdatedAt();

        // Items
        if (order.getItems() != null) {
            int index = 0;
            for (OrderItem item : order.getItems()) {
                OrderItemEntity itemEntity = OrderItemEntity.fromDomain(item);
                itemEntity.setOrder(entity);
                itemEntity.setItemOrder(index++);
                entity.items.add(itemEntity);
            }
        }

        return entity;
    }

    /**
     * OrderEntity를 도메인 Order로 변환.
     */
    public Order toDomain() {
        Order order = new Order();
        order.setId(this.id);
        order.setMemberId(this.memberId);
        order.setOrderNumber(this.orderNumber);
        order.setTotalAmount(this.totalAmount);
        order.setDiscountAmount(this.discountAmount);
        order.setPaymentAmount(this.paymentAmount);
        order.setIdempotencyKey(this.idempotencyKey);
        order.setStatus(this.status);
        order.setCreatedAt(this.createdAt);
        order.setUpdatedAt(this.updatedAt);

        // Items
        if (this.items != null) {
            List<OrderItem> domainItems = new ArrayList<>();
            for (OrderItemEntity itemEntity : this.items) {
                domainItems.add(itemEntity.toDomain());
            }
            order.setItems(domainItems);
        } else {
            order.setItems(new ArrayList<>());
        }

        return order;
    }
}
