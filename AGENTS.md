# AGENTS.md - HAE Shop Development Guide

## 1. Build & Test Commands

### Gradle (Groovy DSL)
```bash
# Build project
./gradlew build

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.hae.shop.domain.order.OrderServiceTest"

# Run a single test method
./gradlew test --tests "com.hae.shop.domain.order.OrderServiceTest.createOrder"

# Run tests with Testcontainers (integration tests)
./gradlew integrationTest

# Lint check
./gradlew checkstyleMain checkstyleTest

# Clean build
./gradlew clean build
```

### Docker Compose (Local Development)
```bash
# Start all services (PostgreSQL, Redis, App)
docker-compose up -d

# Start only infrastructure (PostgreSQL, Redis)
docker-compose up -d postgres redis

# Stop all services
docker-compose down

# View logs
docker-compose logs -f app
```

### IDE Configuration
- **Java Version**: 21 (Amazon Corretto 21 or Zulu JDK 21)
- **Lombok**: Enable annotation processing
- **Gradle**: Use Gradle wrapper (`./gradlew`)

---

## 2. Code Style Guidelines

### 2.1 Architecture: DDD + Hexagonal

Follow **strict layer separation**:

```
src/main/java/com/hae/shop/
├── common/              # Global exceptions, ErrorCode enum, utilities
├── config/              # Spring configuration (Security, JPA, Redis, etc.)
├── domain/              # Pure Java domain models (NO framework dependencies)
│   ├── member/
│   ├── product/
│   ├── order/
│   │   ├── model/       # Entity, VO, Domain Event
│   │   └── port/       # Hexagonal ports
│   │       ├── in/     # UseCase interfaces (Application implements)
│   │       └── out/    # Repository, External API interfaces (Infra implements)
│   └── coupon/
├── application/         # UseCase services (implements port.in)
├── interfaces/          # REST controllers, DTOs, Mappers (v1 prefix)
└── infrastructure/      # Adapters (implements port.out)
    ├── external/        # Payment gateway, email (Resilience4j)
    ├── outbox/          # Transactional Outbox pattern
    └── persistence/     # JPA/Redis repositories, Querydsl
```

**CRITICAL RULES**:
- Domain layer MUST NOT depend on infrastructure (no JPA annotations in domain)
- Use ports (interfaces) to isolate domain from infrastructure
- Controllers MUST return DTOs, NOT domain entities

### 2.2 Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Package | lowercase | `com.hae.shop.domain.order` |
| Class | PascalCase | `OrderService`, `OrderEntity` |
| Interface | PascalCase, suffix `Port` | `OrderRepositoryPort`, `PaymentGatewayPort` |
| Method | camelCase | `createOrder()`, `findById()` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Test Class | `[ClassName]Test` | `OrderServiceTest` |
| Test Method | `[methodName]_[scenario]_[expected]` | `createOrder_whenStockAvailable_shouldSucceed` |

### 2.3 Java 21 Best Practices

**Use Records for DTOs and VOs**:
```java
// Good - Immutable DTO
public record OrderResponse(Long orderId, OrderStatus status, BigDecimal totalAmount) {}

// Good - Value Object
public record Money(BigDecimal amount, Currency currency) {
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}
```

**Virtual Threads**:
- Enable in `application.yml`: `spring.threads.virtual.enabled=true`
- Avoid `synchronized` blocks (causes thread pinning)
- Use `ReentrantLock` or `synchronized` on `java.util.concurrent` primitives
- Minimize `ThreadLocal` usage

### 2.4 Import Organization (IntelliJ IDEA)

```java
// Order: static imports -> java -> javax -> com.hae.shop -> others
import static java.math.RoundingMode.HALF_UP;
import static org.springframework.http.HttpStatus.OK;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.port.in.CreateOrderUseCase;
import com.hae.shop.application.OrderService;
```

### 2.5 Error Handling

**Use ErrorCode Enum** (in `common/`):
```java
public enum ErrorCode {
    ORDER_NOT_FOUND("O001", "주문을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK("O002", "재고가 부족합니다."),
    PAYMENT_FAILED("P001", "결제에 실패했습니다."),
    // ...
}
```

**Global Exception Handler**:
- All exceptions must be caught by `@RestControllerAdvice`
- Return `ErrorResponse` (DTO) with consistent JSON format:
```json
{
  "errorCode": "O001",
  "message": "주문을 찾을 수 없습니다.",
  "timestamp": "2024-01-15T10:30:00"
}
```

### 2.6 API Design

- **Base URL**: `/api/v1/`
- **Versioning**: Always use `v1` prefix
- **Request/Response DTOs**: Use Records
- **Pagination**: Use `Pageable` and return `Page<T>`
- **OpenAPI**: Use `Springdoc-openapi` for Swagger UI at `/swagger-ui.html`

### 2.7 Transactional Outbox Pattern

**Required for ALL async operations** (payment, notifications):

1. Save event to Outbox table within same transaction as business logic
2. Use `@Scheduled` Polling Publisher to process outbox
3. Mark events as processed after successful external call

```java
// Example: OrderService
@Transactional
public OrderResult createOrder(CreateOrderCommand command) {
    // Business logic
    orderRepository.save(order);
    
    // Save to outbox (same transaction)
    outboxRepository.save(OrderCreatedEvent.from(order));
    
    return OrderResult.from(order);
}
```

### 2.8 Idempotency

For order/payment endpoints:
- Accept `Idempotency-Key` header from client
- Check for existing processing/completed requests before processing
- Use Redis or database to track idempotency keys

---

## 3. Testing Requirements

### 3.1 Test Strategy

| Test Type | Framework | When to Use |
|-----------|-----------|-------------|
| Unit | JUnit 5 + Mockito | Business logic in domain/application layers |
| Integration | JUnit 5 + Testcontainers | Repository, controller tests |
| Concurrency | JUnit 5 + ExecutorService | Stock deduction, distributed lock tests |

### 3.2 Testcontainers Configuration

```yaml
# application-test.yml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  redis:
    url: redis:tc://localhost:6379
```

### 3.3 Concurrency Test Example

```java
@Test
void decrementStock_concurrentRequests_shouldHandleAllRequests() throws InterruptedException {
    int threadCount = 100;
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                productService.decrementStock(productId, 1);
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await(10, TimeUnit.SECONDS);
    // Verify final stock
}
```

### 3.4 Architecture Tests (ArchUnit)

```java
@Test
void domain_shouldNotDependOnInfrastructure() {
    noClasses()
        .that().resideInAPackage("com.hae.shop.domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.hae.shop.infrastructure..")
        .check(imports);
}
```

---

## 4. Security & Authentication

- **JWT**: Access + Refresh tokens, stateless
- **Password**: BCrypt encryption
- **Authorization**: RBAC (`ROLE_USER`, `ROLE_ADMIN`)
- **Filter**: Extend `OncePerRequestFilter`

---

## 5. Observability

- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- **JVM Options**: `-Djdk.tracePinnedThreads=full` for virtual thread debugging

---

## 6. IMPORTANT Notes for Agents

1. **NEVER return domain entities from controllers** - Always map to DTO
2. **ALWAYS use Transactional Outbox** for async operations
3. **NEVER use `synchronized`** - Use `ReentrantLock` or Redis distributed locks
4. **ALWAYS add Javadoc** with `@throws` for exception scenarios
5. **USE Testcontainers** for integration tests (not H2)
6. **FOLLOW the layer structure** strictly - domain is pure Java
