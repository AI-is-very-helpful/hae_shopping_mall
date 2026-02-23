---
name: tdd-testcontainers
description: TDD cycle and Testcontainers testing strategy - Red-Green-Refactor, concurrency tests, ArchUnit verification
license: MIT
compatibility: opencode
metadata:
  domain: testing
  priority: critical
  project: hae-shop
---

## What I Do

I guide TDD (Test-Driven Development) practices and Testcontainers-based integration testing for this e-commerce backend. I ensure tests are reliable, repeatable, and catch concurrency issues.

## When to Use Me

Use this skill when:
- Writing new business logic (follow TDD cycle)
- Setting up integration tests
- Testing repository layer
- Writing concurrency/stress tests
- Verifying architecture constraints

## TDD Cycle: Red-Green-Refactor

### 1. Red - Write Failing Test
```java
@Test
void createOrder_whenStockAvailable_shouldSucceed() {
    // Arrange
    Product product = Product.builder()
        .id(1L)
        .name("Test Product")
        .stock(10)
        .price(BigDecimal.valueOf(10000))
        .build();
    
    // Act & Assert - This should FAIL first
    assertThat(product.decreaseStock(5)).isTrue();
    assertThat(product.getStock()).isEqualTo(5);
}
```

### 2. Green - Make Test Pass
```java
// Simple implementation to pass the test
public boolean decreaseStock(int quantity) {
    if (this.stock >= quantity) {
        this.stock -= quantity;
        return true;
    }
    return false;
}
```

### 3. Refactor - Improve Code
```java
// Refactor to proper domain model
public record Stock(int quantity) {
    public Stock decrease(int quantity) {
        if (this.quantity < quantity) {
            throw new DomainException(ErrorCode.INSUFFICIENT_STOCK);
        }
        return new Stock(this.quantity - quantity);
    }
}
```

## Testcontainers Configuration

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  redis:
    url: redis:tc://localhost:6379
  jpa:
    hibernate:
      ddl-auto: create-drop
```

### Test Class Setup
```java
@Testcontainers
@SpringBootTest
class OrderRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.redis.url", () -> "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
    }
}
```

## Concurrency Testing Pattern

Test concurrent stock deduction to verify distributed locking works:

```java
@Test
void decrementStock_concurrentRequests_shouldHandleAllRequests() throws InterruptedException {
    // Given
    Long productId = 1L;
    int threadCount = 100;
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    
    // Prepare test data
    productRepository.save(Product.builder()
        .id(productId)
        .stock(50)
        .build());
    
    // When - submit all requests
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                startLatch.await();
                stockService.decrementStock(productId, 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });
    }
    
    startLatch.countDown();
    assertTrue(endLatch.await(30, TimeUnit.SECONDS));
    
    // Then
    Product product = productRepository.findById(productId).orElseThrow();
    
    // Exactly 50 should succeed, 50 should fail (stock exhausted)
    assertThat(successCount.get()).isEqualTo(50);
    assertThat(failCount.get()).isEqualTo(50);
    assertThat(product.getStock()).isEqualTo(0);
}
```

## ArchUnit Architecture Tests

Verify architecture constraints automatically:

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

@Test
void controllers_shouldBeInInterfacesPackage() {
    classes().that().areAnnotatedWith(Controller.class)
        .should().resideInAPackage("com.hae.shop.interfaces..")
        .check(imports);
}

@Test
void useCases_shouldImplementPortIn() {
    classes().that()
        .areAnnotatedWith(Service.class)
        .should().implementANY(
            classes().that().resideInAPackage("com.hae.shop.domain..port.in")
        )
        .check(imports);
}
```

## Test Naming Convention

Follow this pattern: `[methodName]_[scenario]_[expected]`

| Test Type | Example |
|-----------|---------|
| Success case | `createOrder_whenStockAvailable_shouldSucceed` |
| Failure case | `createOrder_whenInsufficientStock_shouldThrowException` |
| Edge case | `createOrder_whenZeroQuantity_shouldThrowException` |
| Concurrency | `decrementStock_concurrentRequests_shouldHandleAll` |

## Test Categories

| Test Type | Framework | When to Use |
|-----------|-----------|-------------|
| Unit | JUnit 5 + Mockito | Business logic in domain/application layers |
| Integration | JUnit 5 + Testcontainers | Repository, controller tests |
| Concurrency | JUnit 5 + ExecutorService | Stock deduction, distributed lock tests |
| Architecture | ArchUnit | Layer dependency verification |

## Checklist

- [ ] Follow Red-Green-Refactor TDD cycle
- [ ] Use Testcontainers (NOT H2) for integration tests
- [ ] Write concurrency tests for critical paths (stock deduction)
- [ ] Add ArchUnit tests for architecture verification
- [ ] Use proper test naming convention
- [ ] Run `./gradlew test` before committing
- [ ] Run `./gradlew integrationTest` for full test suite
