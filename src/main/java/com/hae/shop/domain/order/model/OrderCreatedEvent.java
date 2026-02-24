package com.hae.shop.domain.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Order가 생성되었을 때 발생하는 도메인 이벤트.
 * Outbox 패턴을 통해 비동기로 처리됩니다.
 */
public record OrderCreatedEvent(
    Long orderId,
    Long memberId,
    String orderNumber,
    BigDecimal totalAmount,
    BigDecimal discountAmount,
    BigDecimal paymentAmount,
    String status,
    List<OrderItem> items,
    Instant createdAt
) {
    /**
     * Order entity에서 OrderCreatedEvent를 생성합니다.
     */
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
            order.getId(),
            order.getMemberId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getPaymentAmount(),
            order.getStatus().name(),
            order.getItems(),
            order.getCreatedAt()
        );
    }
}
