package com.hae.shop.domain.order.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 결제가 완료되었을 때 발생하는 도메인 이벤트.
 * 알림 발송 등의 비동기 작업을 트리거합니다.
 */
public record PaymentCompletedEvent(
    Long orderId,
    String orderNumber,
    Long memberId,
    BigDecimal paymentAmount,
    String paymentMethod,
    Instant paidAt,
    String receiptUrl
) {
    /**
     * Order entity에서 PaymentCompletedEvent를 생성합니다.
     */
    public static PaymentCompletedEvent from(Order order, String paymentMethod, String receiptUrl) {
        return new PaymentCompletedEvent(
            order.getId(),
            order.getOrderNumber(),
            order.getMemberId(),
            order.getPaymentAmount(),
            paymentMethod,
            Instant.now(),
            receiptUrl
        );
    }
}
