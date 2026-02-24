package com.hae.shop.infrastructure.outbox;

import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;
import com.hae.shop.domain.order.port.out.OutboxPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OutboxPort 구현: 도메인 이벤트를 OutboxEvent 테이블에 저장합니다.
 * 동일 트랜잭션 내에서 저장되므로, 비즈니스 로직과 원자적으로 반영됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPortAdapter implements OutboxPort {

    private final OutboxEventMapper outboxEventMapper;
    private final OutboxRepository outboxRepository;

    @Override
    @Transactional
    public void saveOrderCreatedEvent(OrderCreatedEvent event) {
        OutboxEvent outboxEvent = outboxEventMapper.toOutboxEvent(event);
        outboxRepository.save(outboxEvent);
        log.debug("Saved OrderCreatedEvent to outbox: orderId={}", event.orderId());
    }

    @Override
    @Transactional
    public void savePaymentCompletedEvent(PaymentCompletedEvent event) {
        OutboxEvent outboxEvent = outboxEventMapper.toOutboxEvent(event);
        outboxRepository.save(outboxEvent);
        log.debug("Saved PaymentCompletedEvent to outbox: orderId={}", event.orderId());
    }

    @Override
    @Transactional
    public void saveOrderCancelledEvent(OrderCancelledEvent event) {
        OutboxEvent outboxEvent = outboxEventMapper.toOutboxEvent(event);
        outboxRepository.save(outboxEvent);
        log.debug("Saved OrderCancelledEvent to outbox: orderId={}", event.orderId());
    }

    @Override
    @Transactional
    public void saveEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setEventType(eventType);
        outboxEvent.setPayload(payload);
        // createdAt: 자동 설정
        outboxRepository.save(outboxEvent);
        log.debug("Saved generic event to outbox: type={}, aggregateId={}", eventType, aggregateId);
    }
}
