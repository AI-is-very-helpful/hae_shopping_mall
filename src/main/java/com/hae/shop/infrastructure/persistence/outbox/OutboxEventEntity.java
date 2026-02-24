package com.hae.shop.infrastructure.persistence.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.Instant;

/**
 * Outbox Event JPA Entity.
 * Maps to the outbox_events table for Transactional Outbox pattern.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    @Comment("Domain aggregate type (e.g., Order, Payment)")
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    @Comment("Unique identifier of the aggregate")
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    @Comment("Event type name (e.g., OrderCreatedEvent)")
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    @Comment("Serialized event payload (JSON)")
    private String payload;

    @Column(name = "created_at", nullable = false)
    @Comment("Event creation timestamp")
    private Instant createdAt;

    @Column(name = "processed_at")
    @Comment("Timestamp when event was successfully processed")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    @Comment("Number of processing attempts")
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    @Comment("Last error message if processing failed")
    private String lastError;
}
