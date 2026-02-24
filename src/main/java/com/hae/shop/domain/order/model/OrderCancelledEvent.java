package com.hae.shop.domain.order.model;

import java.time.Instant;

/**
 * 주문이 취소되었을 때 발생하는 도메인 이벤트.
 */
public record OrderCancelledEvent(
    Long orderId,
    String orderNumber,
    Long memberId,
    String reason,
    Instant cancelledAt
) {
    /**
     * Order entity에서 OrderCancelledEvent를 생성합니다.
     */
    public static OrderCancelledEvent from(Order order, String reason) {
        return new OrderCancelledEvent(
            order.getId(),
            order.getOrderNumber(),
            order.getMemberId(),
            reason,
            Instant.now()
        );
    }
}
