package com.hae.shop.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.processedAt IS NULL ORDER BY e.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents();

    @Query("SELECT e FROM OutboxEvent e WHERE e.processedAt IS NULL AND e.retryCount < 5 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findRetryableEvents();
}
