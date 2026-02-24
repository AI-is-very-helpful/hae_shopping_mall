package com.hae.shop.domain.order.port.out;

import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;
import com.hae.shop.infrastructure.outbox.OutboxEvent;

/**
 * 도메인 이벤트를 Outbox 테이블에 저장하는 포트.
 */
public interface OutboxPort {

    /**
     * OrderCreatedEvent를 Outbox에 저장합니다.
     */
    void saveOrderCreatedEvent(OrderCreatedEvent event);

    /**
     * PaymentCompletedEvent를 Outbox에 저장합니다.
     */
    void savePaymentCompletedEvent(PaymentCompletedEvent event);

    /**
     * OrderCancelledEvent를 Outbox에 저장합니다.
     */
    void saveOrderCancelledEvent(OrderCancelledEvent event);

    /**
     * 일반적인 도메인 이벤트를 Outbox에 저장합니다.
     */
    void saveEvent(String aggregateType, String aggregateId, String eventType, String payload);
}
