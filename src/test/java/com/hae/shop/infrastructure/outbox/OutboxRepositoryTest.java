package com.hae.shop.infrastructure.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class OutboxRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    @Autowired
    private OutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("Outbox 이벤트 저장 성공")
    void save_shouldPersistEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("Order");
        event.setAggregateId("1");
        event.setEventType("OrderCreated");
        event.setPayload("{\"orderId\": 1, \"amount\": 30000}");

        OutboxEvent saved = outboxRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.isProcessed()).isFalse();
    }

    @Test
    @DisplayName("미처리 이벤트 조회")
    void findUnprocessedEvents_shouldReturnUnprocessedEvents() {
        OutboxEvent event1 = createEvent("Order", "1", "OrderCreated");
        OutboxEvent event2 = createEvent("Order", "2", "OrderCreated");
        OutboxEvent event3 = createEvent("Order", "3", "OrderCreated");

        event2.setProcessedAt(Instant.now());

        outboxRepository.save(event1);
        outboxRepository.save(event2);
        outboxRepository.save(event3);

        List<OutboxEvent> unprocessed = outboxRepository.findUnprocessedEvents();

        assertThat(unprocessed).hasSize(2);
    }

    @Test
    @DisplayName("재시도 가능한 이벤트 조회 - retryCount < 5")
    void findRetryableEvents_shouldReturnRetryableEvents() {
        OutboxEvent event1 = createEvent("Order", "1", "OrderCreated");
        event1.setRetryCount(3);
        event1.setLastError("Temporary failure");

        OutboxEvent event2 = createEvent("Order", "2", "OrderCreated");
        event2.setRetryCount(5);

        outboxRepository.save(event1);
        outboxRepository.save(event2);

        List<OutboxEvent> retryable = outboxRepository.findRetryableEvents();

        assertThat(retryable).hasSize(1);
        assertThat(retryable.get(0).getAggregateId()).isEqualTo("1");
    }

    @Test
    @DisplayName("이벤트 처리 완료 - processedAt 설정")
    void save_withProcessedAt_shouldMarkAsProcessed() {
        OutboxEvent event = createEvent("Order", "1", "OrderCreated");
        OutboxEvent saved = outboxRepository.save(event);

        saved.setProcessedAt(Instant.now());
        OutboxEvent updated = outboxRepository.save(saved);

        assertThat(updated.isProcessed()).isTrue();
        assertThat(updated.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("이벤트 재시도 횟수 증가")
    void save_withRetryCount_shouldPersist() {
        OutboxEvent event = createEvent("Order", "1", "OrderCreated");
        OutboxEvent saved = outboxRepository.save(event);

        saved.setRetryCount(1);
        saved.setLastError("First attempt failed");
        OutboxEvent updated = outboxRepository.save(saved);

        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getLastError()).isEqualTo("First attempt failed");
    }

    private OutboxEvent createEvent(String aggregateType, String aggregateId, String eventType) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload("{\"id\": " + aggregateId + "}");
        return event;
    }
}
