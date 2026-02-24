package com.hae.shop.infrastructure.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingPublisher {

    private final OutboxRepository outboxRepository;

    @Value("${outbox.polling.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${outbox.polling.interval:3000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxRepository.findUnprocessedEvents()
            .stream()
            .limit(batchSize)
            .toList();

        if (events.isEmpty()) {
            return;
        }

        log.info("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
                log.info("Successfully processed event: {} - {}", event.getAggregateType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to process event: {}", event.getId(), e);
                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage());
                outboxRepository.save(event);
            }
        }
    }

    private void processEvent(OutboxEvent event) {
        log.debug("Processing event: type={}, aggregateId={}, payload={}", 
            event.getEventType(), event.getAggregateId(), event.getPayload());
    }
}
