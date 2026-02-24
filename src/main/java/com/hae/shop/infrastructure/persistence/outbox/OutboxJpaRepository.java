package com.hae.shop.infrastructure.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * JPA Repository for OutboxEventEntity.
 * Provides data access methods for outbox event persistence and retrieval.
 */
@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Find unprocessed outbox events (processed_at IS NULL).
     * Ordered by creation timestamp to ensure chronological processing.
     *
     * @param limit Maximum number of events to retrieve
     * @return List of unprocessed outbox events
     */
    List<OutboxEventEntity> findByProcessedAtIsNullOrderByCreatedAtAsc(int limit);

    /**
     * Find unprocessed events by aggregate type (e.g., Order, Payment).
     *
     * @param aggregateType Type of aggregate
     * @param limit Maximum number of events
     * @return List of unprocessed events for the given aggregate type
     */
    List<OutboxEventEntity> findByAggregateTypeAndProcessedAtIsNullOrderByCreatedAtAsc(String aggregateType, int limit);

    /**
     * Count unprocessed events.
     *
     * @return Number of events where processed_at IS NULL
     */
    long countByProcessedAtIsNull();
}
