package com.hae.shop.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 매퍼: 도메인 이벤트 → OutboxEvent 변환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventMapper {

    private final ObjectMapper objectMapper;

    public OutboxEvent toOutboxEvent(OrderCreatedEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Order");
            outboxEvent.setAggregateId(event.orderId().toString());
            outboxEvent.setEventType("OrderCreated");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            // createdAt: 자동 설정 (@PrePersist)
            outboxEvent.setProcessedAt(null);
            outboxEvent.setRetryCount(0);
            outboxEvent.setLastError(null);
            return outboxEvent;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent: {}", event, e);
            throw new IllegalArgumentException("Failed to serialize event", e);
        }
    }

    public OutboxEvent toOutboxEvent(PaymentCompletedEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Order");
            outboxEvent.setAggregateId(event.orderId().toString());
            outboxEvent.setEventType("PaymentCompleted");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            // createdAt: 자동 설정 (@PrePersist)
            outboxEvent.setProcessedAt(null);
            outboxEvent.setRetryCount(0);
            outboxEvent.setLastError(null);
            return outboxEvent;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PaymentCompletedEvent: {}", event, e);
            throw new IllegalArgumentException("Failed to serialize event", e);
        }
    }

    public OutboxEvent toOutboxEvent(OrderCancelledEvent event) {
        try {
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("Order");
            outboxEvent.setAggregateId(event.orderId().toString());
            outboxEvent.setEventType("OrderCancelled");
            outboxEvent.setPayload(objectMapper.writeValueAsString(event));
            // createdAt: 자동 설정 (@PrePersist)
            outboxEvent.setProcessedAt(null);
            outboxEvent.setRetryCount(0);
            outboxEvent.setLastError(null);
            return outboxEvent;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCancelledEvent: {}", event, e);
            throw new IllegalArgumentException("Failed to serialize event", e);
        }
    }
}
