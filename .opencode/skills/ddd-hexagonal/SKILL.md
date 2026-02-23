---
name: ddd-hexagonal
description: Enforce DDD + Hexagonal Architecture rules - domain purity, port patterns, layer separation, DTO isolation
license: MIT
compatibility: opencode
metadata:
  domain: architecture
  priority: critical
  project: hae-shop
---

## What I Do

I enforce strict DDD + Hexagonal Architecture rules for this Spring Boot e-commerce backend project. I ensure clean separation between domain logic and infrastructure concerns.

## When to Use Me

Use this skill when:
- Creating new domain entities, value objects, or domain services
- Defining ports (interfaces) between layers
- Implementing application services or infrastructure adapters
- Reviewing code for architecture violations
- Setting up new bounded contexts (member, product, order, coupon)

## Architecture Overview

```
src/main/java/com/hae/shop/
├── common/              # Global exceptions, ErrorCode enum, utilities
├── config/               # Spring configuration (Security, JPA, Redis, etc.)
├── domain/               # PURE JAVA - NO framework dependencies
│   ├── member/
│   ├── product/
│   ├── order/
│   │   ├── model/       # Entity, VO, Domain Event (PURE JAVA)
│   │   └── port/
│   │       ├── in/       # UseCase interfaces (Application implements)
│   │       └── out/      # Repository, External API interfaces (Infra implements)
│   └── coupon/
├── application/          # UseCase services (implements port.in)
├── interfaces/           # REST controllers, DTOs, Mappers
└── infrastructure/       # Adapters (implements port.out)
    ├── external/         # Payment gateway, email (Resilience4j)
    ├── outbox/          # Transactional Outbox pattern
    └── persistence/      # JPA/Redis repositories, Querydsl
```

## CRITICAL RULES (NEVER VIOLATE)

### 1. Domain Purity
```java
// ❌ FORBIDDEN - JPA annotations in domain
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue
    private Long id;
}

// ✅ CORRECT - Pure Java domain model
public class Order {
    private OrderId id;
    private OrderStatus status;
    private List<OrderLine> orderLines;
    
    // Business logic methods only
    public void cancel() {
        if (status != OrderStatus.PENDING) {
            throw new DomainException(ErrorCode.ORDER_CANNOT_BE_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
    }
}
```

### 2. Port Naming Convention
```java
// ✅ Port interfaces use "Port" suffix
// port/in/ - UseCase interfaces (Application layer implements)
public interface CreateOrderUseCase {
    OrderResult createOrder(CreateOrderCommand command);
}

// port/out/ - Infrastructure interfaces (Infrastructure layer implements)
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
}

public interface PaymentGatewayPort {
    PaymentResult pay(PaymentCommand command);
}
```

### 3. DTO Isolation from Controllers
```java
// ❌ FORBIDDEN - Returning domain entity from controller
@GetMapping("/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderService.findById(id); // Entity leaked!
}

// ✅ CORRECT - Return DTO
@GetMapping("/{id}")
public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderService.findById(id);
    return OrderResponse.from(order);
}

// Use Records for DTOs
public record OrderResponse(
    Long orderId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderLineResponse> orderLines
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId().value(),
            order.getStatus(),
            order.getTotalAmount().amount(),
            order.getOrderLines().stream()
                .map(OrderLineResponse::from)
                .toList()
        );
    }
}
```

### 4. Layer Dependency Direction
```
Infrastructure → Application → Domain
     ↓              ↓           ↓
  (depends on)  (depends on)  (no dependencies)

CORRECT: Infrastructure depends on Domain
WRONG: Domain depends on Infrastructure
```

## Checklist for New Code

Before committing new code, verify:

- [ ] Domain models have NO JPA/Spring annotations
- [ ] Domain models contain ONLY business logic
- [ ] Port interfaces are in `domain/*/port/` directory
- [ ] Repository interfaces are in `port/out/` with `Port` suffix
- [ ] Controllers return DTOs, never entities
- [ ] Infrastructure adapters implement `port.out` interfaces
- [ ] Application services implement `port.in` interfaces
- [ ] No direct instantiation of infrastructure classes in domain/application

## ArchUnit Verification

Run architecture tests to verify compliance:

```bash
./gradlew test --tests "*ArchUnit*"
```

```java
@Test
void domain_shouldNotDependOnInfrastructure() {
    noClasses()
        .that().resideInAPackage("com.hae.shop.domain..")
        .should().dependOnClassesThat()
        .resideInAPackage("com.hae.shop.infrastructure..")
        .check(imports);
}

@Test
void domain_shouldNotUseSpringAnnotations() {
    noClasses()
        .that().resideInAPackage("com.hae.shop.domain..")
        .should().beAnnotatedWith("org.springframework.stereotype.Component")
        .orShould().beAnnotatedWith("jakarta.persistence.Entity")
        .check(imports);
}
```
