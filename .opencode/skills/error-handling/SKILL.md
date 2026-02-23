---
name: error-handling
description: Global error handling patterns - ErrorCode enum, @RestControllerAdvice, consistent ErrorResponse format
license: MIT
compatibility: opencode
metadata:
  domain: error-handling
  priority: high
  project: hae-shop
---

## What I Do

I implement consistent error handling across the entire application. All errors follow a standardized format, making it easy for clients to handle exceptions programmatically.

## When to Use Me

Use this skill when:
- Creating new exception types
- Handling business logic errors
- Setting up global exception handling
- Adding error documentation

## ErrorCode Enum

Define all business error codes in a central enum:

```java
// common/ErrorCode.java
public enum ErrorCode {
    // Order errors (Oxxx)
    ORDER_NOT_FOUND("O001", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELLED("O002", "이미 취소된 주문입니다."),
    ORDER_CANNOT_BE_CANCELLED("O003", "취소할 수 없는 주문 상태입니다."),
    INSUFFICIENT_STOCK("O004", "재고가 부족합니다."),
    
    // Product errors (Pxxx)
    PRODUCT_NOT_FOUND("P001", "상품을 찾을 수 없습니다."),
    PRODUCT_OUT_OF_STOCK("P002", "품절된 상품입니다."),
    
    // Payment errors (PAYxxx)
    PAYMENT_FAILED("PAY001", "결제에 실패했습니다."),
    PAYMENT_TIMEOUT("PAY002", "결제 시간이 초과되었습니다."),
    PAYMENT_CANCEL_FAILED("PAY003", "결제 취소에 실패했습니다."),
    
    // Member errors (Mxxx)
    MEMBER_NOT_FOUND("M001", "회원을 찾을 수 없습니다."),
    INVALID_PASSWORD("M002", "비밀번호가 일치하지 않습니다."),
    DUPLICATE_EMAIL("M003", "이미 사용 중인 이메일입니다."),
    
    // Common errors (Cxxx)
    INVALID_REQUEST("C001", "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR("C999", "서버 내부 오류가 발생했습니다.");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
```

## Custom Exceptions

### BusinessException
```java
// common/BusinessException.java
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
```

### NotFoundException
```java
// common/NotFoundException.java
public class NotFoundException extends BusinessException {
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

## Global Exception Handler

```java
// common/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    // Handle BusinessException
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getErrorCode(), e);
        
        ErrorResponse response = ErrorResponse.of(
            e.getErrorCode().getCode(),
            e.getErrorCode().getMessage()
        );
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    // Handle NotFoundException
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException e) {
        log.warn("Not found: {}", e.getErrorCode(), e);
        
        ErrorResponse response = ErrorResponse.of(
            e.getErrorCode().getCode(),
            e.getErrorCode().getMessage()
        );
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }
    
    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation error: {}", e.getMessage());
        
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("유효성 검증에 실패했습니다.");
        
        ErrorResponse response = ErrorResponse.of("V001", message);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    // Handle unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected error", e);
        
        ErrorResponse response = ErrorResponse.of(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
```

## ErrorResponse DTO

```java
// common/ErrorResponse.java
public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, LocalDateTime.now());
    }
}
```

## Consistent JSON Response Format

```json
// Error Response
{
  "errorCode": "O001",
  "message": "주문을 찾을 수 없습니다.",
  "timestamp": "2024-01-15T10:30:00"
}

// Validation Error
{
  "errorCode": "V001",
  "message": "memberId: 필수 값입니다.",
  "timestamp": "2024-01-15T10:30:00"
}
```

## Javadoc with @throws

Always document exception scenarios in Javadoc:

```java
/**
 * Creates a new order for the given command.
 *
 * @param command the order creation command containing member and product details
 * @return the created order result
 * @throws BusinessException if stock is insufficient (ErrorCode: INSUFFICIENT_STOCK)
 * @throws BusinessException if product is out of stock (ErrorCode: PRODUCT_OUT_OF_STOCK)
 * @throws NotFoundException if member not found (ErrorCode: MEMBER_NOT_FOUND)
 * @throws NotFoundException if product not found (ErrorCode: PRODUCT_NOT_FOUND)
 */
public OrderResult createOrder(CreateOrderCommand command) {
    // ...
}
```

## Checklist

- [ ] All business errors defined in ErrorCode enum with prefix (O, P, M, etc.)
- [ ] Custom exception classes extend RuntimeException
- [ ] GlobalExceptionHandler catches all exceptions
- [ ] ErrorResponse uses consistent JSON format
- [ ] Proper HTTP status codes (400, 404, 500, etc.)
- [ ] Logging at appropriate levels (warn for business, error for system)
- [ ] @throws documented in Javadoc for all public methods
- [ ] Validation errors handled with meaningful messages
