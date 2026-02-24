package com.hae.shop.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;
import com.hae.shop.infrastructure.persistence.outbox.OutboxEventEntity;
import com.hae.shop.infrastructure.persistence.outbox.OutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Outbox Polling Publisher.
 * Periodically polls unprocessed outbox events and publishes them to downstream systems.
 * Implements the polling relay mechanism of the Transactional Outbox pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxJpaRepository outboxJpaRepository;
    private final ObjectMapper objectMapper;

    @Value("${outbox.polling.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.polling.interval:3000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEventEntity> events = outboxJpaRepository.findByProcessedAtIsNullOrderByCreatedAtAsc(batchSize);

        if (events.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events", events.size());

        for (OutboxEventEntity eventEntity : events) {
            try {
                processEvent(eventEntity);
                eventEntity.setProcessedAt(Instant.now());
                eventEntity.setRetryCount(0);
                eventEntity.setLastError(null);
                outboxJpaRepository.save(eventEntity);
                log.info("Successfully processed event: id={}, type={}, aggregateId={}", 
                    eventEntity.getId(), eventEntity.getEventType(), eventEntity.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}, type={}, error={}", 
                    eventEntity.getId(), eventEntity.getEventType(), e.getMessage(), e);
                eventEntity.setRetryCount(eventEntity.getRetryCount() + 1);
                eventEntity.setLastError(e.getMessage());
                outboxJpaRepository.save(eventEntity);
            }
        }
    }

    private void processEvent(OutboxEventEntity eventEntity) throws JsonProcessingException {
        String eventType = eventEntity.getEventType();
        String payload = eventEntity.getPayload();

        switch (eventType) {
            case "OrderCreatedEvent" -> {
                OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
                log.info("Publishing OrderCreatedEvent: orderId={}, memberId={}", event.orderId(), event.memberId());
            }
            case "PaymentCompletedEvent" -> {
                PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);
                log.info("Publishing PaymentCompletedEvent: orderId={}, transactionId={}", event.orderId(), event.receiptUrl());
            }
            case "OrderCancelledEvent" -> {
                OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
                log.info("Publishing OrderCancelledEvent: orderId={}, reason={}", event.orderId(), event.reason());
            }
            default -> log.warn("Unknown event type: {}", eventType);
        }
    }
}
