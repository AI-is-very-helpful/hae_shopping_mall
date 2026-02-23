---
name: spring-boot-api
description: REST API design best practices - v1 prefix, Records for DTOs, pagination, OpenAPI, idempotency handling
license: MIT
compatibility: opencode
metadata:
  domain: api
  priority: high
  project: hae-shop
---

## What I Do

I define REST API design standards for consistent, maintainable, and well-documented APIs in this Spring Boot backend.

## When to Use Me

Use this skill when:
- Creating new REST endpoints
- Designing Request/Response DTOs
- Implementing pagination
- Setting up OpenAPI documentation
- Handling idempotency for payment/order endpoints

## API Design Rules

### Base URL and Versioning
```java
// ✅ CORRECT - Use v1 prefix
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController { }

// ❌ WRONG - No version, ambiguous
@RestController
@RequestMapping("/orders")
public class OrderController { }
```

### Request DTOs - Use Records
```java
// ✅ CORRECT - Immutable request DTO
public record CreateOrderRequest(
    Long memberId,
    @Valid List<OrderLineRequest> orderLines,
    String couponCode,
    String idempotencyKey
) {}

public record OrderLineRequest(
    Long productId,
    int quantity
) {}
```

### Response DTOs - Use Records
```java
// ✅ CORRECT - Immutable response DTO
public record OrderResponse(
    Long orderId,
    OrderStatus status,
    BigDecimal totalAmount,
    List<OrderLineResponse> orderLines,
    LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId().value(),
            order.getStatus(),
            order.getTotalAmount().amount(),
            order.getOrderLines().stream()
                .map(OrderLineResponse::from)
                .toList(),
            order.getCreatedAt()
        );
    }
}
```

## Pagination

### Controller with Pageable
```java
@GetMapping
public Page<OrderResponse> getOrders(
    @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
    @RequestParam(required = false) OrderStatus status
) {
    Page<Order> orders = orderService.findOrders(status, pageable);
    return orders.map(OrderResponse::from);
}
```

### Response with Metadata
```java
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    boolean hasPrevious
) {
    public static <T> PagedResponse from(Page<T> page) {
        return new PagedResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }
}
```

## OpenAPI Documentation

### Enable Swagger
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("HAE Shop API")
                .version("1.0")
                .description("E-commerce Backend API"));
    }
}
```

### API Documentation on Endpoints
```java
@Operation(
    summary = "Create new order",
    description = "Creates a new order with idempotency support",
    responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Duplicate order (idempotency key conflict)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    }
)
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
) {
    // ...
}
```

## Idempotency Handling

### Why Idempotency?
Prevents duplicate orders/payments due to network retries.

### Implementation Pattern
```java
@Service
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofHours(24);
    
    /**
     * Check if request is duplicate.
     * @return true if duplicate (should skip processing)
     */
    public boolean isDuplicate(String key) {
        Boolean exists = redisTemplate.hasKey("idempotent:" + key);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * Mark request as processing.
     * @return true if marked successfully, false if already exists
     */
    public boolean markProcessing(String key) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue()
                .setIfAbsent("idempotent:" + key, "PROCESSING", TTL)
        );
    }
    
    public void markCompleted(String key) {
        redisTemplate.opsForValue().set("idempotent:" + key, "COMPLETED", TTL);
    }
}
```

### Controller Usage
```java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(
    @Valid @RequestBody CreateOrderRequest request,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
) {
    // Check idempotency
    if (idempotencyKey != null) {
        if (idempotencyService.isDuplicate(idempotencyKey)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
        if (!idempotencyService.markProcessing(idempotencyKey)) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }
    }
    
    try {
        OrderResult result = orderService.createOrder(
            CreateOrderCommand.from(request)
        );
        
        if (idempotencyKey != null) {
            idempotencyService.markCompleted(idempotencyKey);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(OrderResponse.from(result));
    } catch (Exception e) {
        if (idempotencyKey != null) {
            idempotencyService.markFailed(idempotencyKey);
        }
        throw e;
    }
}
```

## Standard API Response Wrapper

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorDetail error,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }
    
    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }
}

public record ErrorDetail(
    String errorCode,
    String message
) {}
```

## Checklist

- [ ] All endpoints use `/api/v1/` prefix
- [ ] Request/Response DTOs use Java Records
- [ ] Use `@Valid` for bean validation
- [ ] Implement pagination with `Pageable`
- [ ] Add OpenAPI annotations (`@Operation`, `@ApiResponse`)
- [ ] Handle idempotency for POST endpoints (order, payment)
- [ ] Return consistent `ApiResponse` wrapper
- [ ] Use proper HTTP status codes (201 Created, 400 Bad Request, etc.)
