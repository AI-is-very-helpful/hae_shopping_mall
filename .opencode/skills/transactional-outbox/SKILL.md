---
name: transactional-outbox
description: Transactional Outbox pattern for reliable async event processing - prevents message loss in payment, notifications
license: MIT
compatibility: opencode
metadata:
  domain: messaging
  priority: critical
  project: hae-shop
---

## What I Do

I implement the Transactional Outbox pattern to ensure reliable async event processing. This guarantees that events (payment notifications, order confirmations) are never lost, even if external systems fail.

## When to Use Me

Use this skill when:
- Implementing payment processing (PG integration)
- Sending notifications (email, SMS, push)
- Publishing domain events to external systems
- Any operation that requires both DB changes AND external calls

## The Problem

```java
// ❌ WRONG - Event can be lost if external call fails
@Transactional
public OrderResult createOrder(CreateOrderCommand command) {
    Order order = orderRepository.save(new Order(...));
    
    // If this fails, order is saved but notification is lost!
    notificationService.sendOrderConfirmation(order);
    
    return OrderResult.from(order);
}
```

## The Solution: Transactional Outbox

### Architecture

```
1. Business Transaction
   └── Save Order
   └── Save Event to Outbox (SAME transaction)
   
2. Polling Publisher (@Scheduled)
   └── Read pending events from Outbox
   └── Send to external system
   └── Mark as processed
```

### Implementation

#### 1. Outbox Entity (Infrastructure Layer)

```java
// infrastructure/outbox/OutboxEntity.java
@Entity
@Table(name = "outbox")
public class OutboxEntity {
    @Id
    private UUID id;
    
    private String aggregateType;     // "ORDER", "PAYMENT", etc.
    private String aggregateId;       // Business entity ID
    private String eventType;         // "ORDER_CREATED", "PAYMENT_COMPLETED"
    
    @Column(columnDefinition = "jsonb")
    private String payload;           // JSON serialized event
    
    private OutboxStatus status;      // PENDING, PROCESSED, FAILED
    
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    
    public static OutboxEntity create(String aggregateType, String aggregateId, 
                                       String eventType, Object payload) {
        OutboxEntity outbox = new OutboxEntity();
        outbox.id = UUID.randomUUID();
        outbox.aggregateType = aggregateType;
        outbox.aggregateId = aggregateId;
        outbox.eventType = eventType;
        outbox.payload = JsonUtils.toJson(payload);
        outbox.status = OutboxStatus.PENDING;
        outbox.retryCount = 0;
        outbox.createdAt = LocalDateTime.now();
        return outbox;
    }
    
    public void markProcessed() {
        this.status = OutboxStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }
    
    public void incrementRetry() {
        this.retryCount++;
        if (this.retryCount >= 3) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
```

#### 2. Domain Event Interface

```java
// domain/common/DomainEvent.java
public interface DomainEvent {
    String eventType();
    String aggregateId();
    Object payload();
}

// domain/order/model/event/OrderCreatedEvent.java
public record OrderCreatedEvent(
    Long orderId,
    Long memberId,
    BigDecimal totalAmount,
    LocalDateTime occurredAt
) implements DomainEvent {
    @Override
    public String eventType() {
        return "ORDER_CREATED";
    }
    
    @Override
    public String aggregateId() {
        return orderId.toString();
    }
    
    @Override
    public Object payload() {
        return this;
    }
    
    public static OrderCreatedEvent from(Order order) {
        return new OrderCreatedEvent(
            order.getId().value(),
            order.getMemberId().value(),
            order.getTotalAmount().amount(),
            LocalDateTime.now()
        );
    }
}
```

#### 3. Application Service with Outbox

```java
// application/OrderService.java
@Service
@Transactional
public class OrderService implements CreateOrderUseCase {
    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    
    @Override
    public OrderResult createOrder(CreateOrderCommand command) {
        // 1. Business logic
        Order order = Order.create(
            new MemberId(command.memberId()),
            command.orderLines(),
            command.couponCode()
        );
        
        order = orderRepository.save(order);
        
        // 2. Save event to outbox (SAME transaction)
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        OutboxEntity outbox = OutboxEntity.create(
            "ORDER",
            order.getId().value().toString(),
            event.eventType(),
            event.payload()
        );
        outboxRepository.save(outbox);
        
        return OrderResult.from(order);
    }
}
```

#### 4. Polling Publisher

```java
// infrastructure/outbox/OutboxPublisher.java
@Component
@Slf4j
public class OutboxPublisher {
    private final OutboxRepositoryPort outboxRepository;
    private final NotificationService notificationService;
    private final PaymentGatewayPort paymentGateway;
    
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 3;
    
    @Scheduled(fixedDelay = 3000) // Every 3 seconds
    public void processPendingEvents() {
        List<OutboxEntity> pendingEvents = outboxRepository
            .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, PageRequest.of(0, BATCH_SIZE));
        
        for (OutboxEntity event : pendingEvents) {
            try {
                processEvent(event);
                event.markProcessed();
                outboxRepository.save(event);
                log.info("Processed outbox event: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getId(), e);
                event.incrementRetry();
                outboxRepository.save(event);
            }
        }
    }
    
    private void processEvent(OutboxEntity event) {
        switch (event.getEventType()) {
            case "ORDER_CREATED" -> handleOrderCreated(event);
            case "PAYMENT_COMPLETED" -> handlePaymentCompleted(event);
            case "ORDER_CANCELLED" -> handleOrderCancelled(event);
            default -> log.warn("Unknown event type: {}", event.getEventType());
        }
    }
    
    private void handleOrderCreated(OutboxEntity event) {
        OrderCreatedEvent payload = JsonUtils.fromJson(event.getPayload(), OrderCreatedEvent.class);
        notificationService.sendOrderConfirmation(payload.memberId(), payload.orderId());
    }
    
    private void handlePaymentCompleted(OutboxEntity event) {
        PaymentCompletedEvent payload = JsonUtils.fromJson(event.getPayload(), PaymentCompletedEvent.class);
        notificationService.sendPaymentConfirmation(payload.memberId(), payload.orderId(), payload.amount());
    }
}
```

### Outbox Table Schema

```sql
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    
    INDEX idx_outbox_status_created (status, created_at)
);

-- For cleanup of processed events (run periodically)
DELETE FROM outbox 
WHERE status = 'PROCESSED' 
AND processed_at < NOW() - INTERVAL '7 days';
```

## Checklist

When implementing async operations:

- [ ] Event saved to outbox in SAME transaction as business logic
- [ ] Polling publisher configured with @Scheduled
- [ ] Retry mechanism with max retry count
- [ ] Events marked as PROCESSED after successful external call
- [ ] Failed events marked with FAILED status after max retries
- [ ] Logging for debugging and monitoring
- [ ] Index on (status, created_at) for efficient polling

## Common Mistakes to Avoid

| Mistake | Solution |
|---------|----------|
| Calling external API in same transaction | Use outbox pattern |
| No retry mechanism | Implement retry count and max attempts |
| Events never cleaned up | Schedule periodic cleanup job |
| No monitoring | Add logging and metrics for outbox size |
