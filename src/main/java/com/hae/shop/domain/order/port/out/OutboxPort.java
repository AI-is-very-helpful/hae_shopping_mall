package com.hae.shop.domain.order.port.out;

import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;

/**
 * Outbox Port - Interface for persisting domain events to the outbox table.
 * Part of the Transactional Outbox pattern to ensure reliable async event delivery.
 */
public interface OutboxPort {

    /**
     * Saves an OrderCreatedEvent to the outbox.
     *
     * @param event The order creation domain event
     * @throws IllegalArgumentException if event is null
     */
    void saveOrderCreatedEvent(OrderCreatedEvent event);

    /**
     * Saves a PaymentCompletedEvent to the outbox.
     *
     * @param event The payment completion domain event
     * @throws IllegalArgumentException if event is null
     */
    void savePaymentCompletedEvent(PaymentCompletedEvent event);

    /**
     * Saves an OrderCancelledEvent to the outbox.
     *
     * @param event The order cancellation domain event
     * @throws IllegalArgumentException if event is null
     */
    void saveOrderCancelledEvent(OrderCancelledEvent event);

    /**
     * Saves a generic event to the outbox.
     *
     * @param aggregateType Type of aggregate (e.g., Order, Payment)
     * @param aggregateId Unique identifier of the aggregate
     * @param eventType Event type name
     * @param payload Serialized event payload (JSON)
     */
    void saveEvent(String aggregateType, String aggregateId, String eventType, String payload);
}
