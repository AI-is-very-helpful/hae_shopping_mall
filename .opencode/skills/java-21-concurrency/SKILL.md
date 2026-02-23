---
name: java-21-concurrency
description: Java 21 Virtual Threads & concurrency best practices - avoid synchronized, use ReentrantLock, Redis distributed locks
license: MIT
compatibility: opencode
metadata:
  domain: concurrency
  priority: critical
  project: hae-shop
---

## What I Do

I guide proper use of Java 21 concurrency features, especially Virtual Threads, to ensure high performance and avoid thread pinning issues in this e-commerce backend.

## When to Use Me

Use this skill when:
- Implementing concurrent operations (time sales, stock deduction)
- Setting up Virtual Threads configuration
- Using Redis distributed locks (Redisson)
- Writing concurrent tests with ExecutorService
- Debugging thread pinning issues

## Virtual Threads Configuration

### Enable in application.yml
```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### JVM Options for Debugging
```bash
# Trace pinned threads
-Djdk.tracePinnedThreads=full

# Monitor virtual threads
-Djdk.virtualThreadScheduler.parallelism=8
-Djdk.virtualThreadScheduler.maxPoolSize=256
```

## CRITICAL: Avoid Thread Pinning

### ❌ FORBIDDEN - synchronized causes pinning
```java
// WRONG - Pins virtual thread to carrier thread
public class StockService {
    private final Object lock = new Object();
    
    public void decrementStock(Long productId, int quantity) {
        synchronized (lock) {  // ❌ PINNING!
            // ... business logic
        }
    }
}
```

### ✅ CORRECT - Use ReentrantLock
```java
import java.util.concurrent.locks.ReentrantLock;

public class StockService {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void decrementStock(Long productId, int quantity) {
        lock.lock();
        try {
            // ... business logic
        } finally {
            lock.unlock();
        }
    }
}
```

### ✅ BETTER - Use Redis Distributed Lock (Redisson)
```java
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@Service
public class StockService {
    private final RedissonClient redisson;
    
    public void decrementStock(Long productId, int quantity) {
        RLock lock = redisson.getLock("stock:" + productId);
        try {
            // Wait up to 10 seconds, hold for 30 seconds
            if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                try {
                    // ... business logic
                } finally {
                    lock.unlock();
                }
            } else {
                throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CONCURRENT_OPERATION_INTERRUPTED);
        }
    }
}
```

## Distributed Lock Pattern for Time Sales

```java
@Service
public class TimeSaleService {
    private final RedissonClient redisson;
    private final ProductRepositoryPort productRepository;
    
    /**
     * Handles concurrent purchase requests during time sale.
     * Uses Redis Pub/Sub distributed lock to prevent DB connection pool exhaustion.
     */
    @Transactional
    public PurchaseResult purchaseTimeSaleProduct(Long productId, Long memberId, int quantity) {
        String lockKey = "timesale:product:" + productId;
        RLock lock = redisson.getLock(lockKey);
        
        try {
            // Try to acquire lock (wait 5s, hold 10s)
            if (!lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.TIMESALE_LOCK_FAILED);
            }
            
            // Critical section - check stock and deduct
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND));
            
            if (product.getStock() < quantity) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            
            product.decreaseStock(quantity);
            productRepository.save(product);
            
            return PurchaseResult.success(productId, quantity);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.CONCURRENT_OPERATION_INTERRUPTED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

## Minimize ThreadLocal Usage

```java
// ❌ AVOID - ThreadLocal with virtual threads
// Virtual threads can be created/destroyed frequently, 
// ThreadLocal values may not behave as expected
private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

// ✅ BETTER - Pass context explicitly or use ScopedValue (Java 21+)
public void processOrder(OrderCommand command, UserContext context) {
    // Pass context as parameter
}

// ✅ OR use request-scoped beans in Spring
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UserContext {
    private Long memberId;
    // ...
}
```

## Concurrency Testing Pattern

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
    
    // When
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                startLatch.await(); // Wait for all threads to be ready
                stockService.decrementStock(productId, 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                endLatch.countDown();
            }
        });
    }
    
    startLatch.countDown(); // Release all threads simultaneously
    assertTrue(endLatch.await(30, TimeUnit.SECONDS));
    
    // Then - verify final stock
    Product product = productRepository.findById(productId).orElseThrow();
    assertThat(product.getStock()).isEqualTo(initialStock - successCount.get());
}
```

## Checklist

Before committing concurrent code:

- [ ] NO `synchronized` blocks used
- [ ] Using `ReentrantLock` for in-process locking
- [ ] Using Redis distributed lock (Redisson) for distributed scenarios
- [ ] ThreadLocal usage minimized or avoided
- [ ] Virtual threads enabled in application.yml
- [ ] Lock timeout configured to prevent deadlocks
- [ ] Proper try-finally for lock release
- [ ] InterruptedException handled correctly
- [ ] Concurrency test written for critical paths
