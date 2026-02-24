package com.hae.shop.infrastructure.outbox;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hae.shop.infrastructure.persistence.outbox.OutboxEventEntity;
import com.hae.shop.infrastructure.persistence.outbox.OutboxJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPollingPublisher {

    private final OutboxJpaRepository outboxJpaRepository;

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

    private void processEvent(OutboxEventEntity eventEntity) {
        log.debug("Processing event: type={}, aggregateId={}, payload={}", 
            eventEntity.getEventType(), eventEntity.getAggregateId(), eventEntity.getPayload());
    }
}
