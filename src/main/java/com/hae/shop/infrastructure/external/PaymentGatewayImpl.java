package com.hae.shop.infrastructure.external;

import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentGatewayImpl implements PaymentGatewayPort {

    private final RestTemplate restTemplate;

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "paymentGateway")
    @TimeLimiter(name = "paymentGateway")
    public PaymentResult processPayment(Long orderId, BigDecimal amount) {
        log.info("Processing payment for orderId={}, amount={}", orderId, amount);
        
        // Mock implementation - in production, call external PG API
        // Example: restTemplate.postForObject(pgUrl, request, PaymentResponse.class);
        
        String transactionId = "TXN-" + System.currentTimeMillis();
        log.info("Payment successful: orderId={}, transactionId={}", orderId, transactionId);
        
        return new PaymentResult(true, transactionId, null);
    }

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "fallbackCancelPayment")
    @Retry(name = "paymentGateway")
    public PaymentResult cancelPayment(String transactionId) {
        log.info("Cancelling payment: transactionId={}", transactionId);
        
        // Mock implementation
        String newTransactionId = "TXN-CANCEL-" + System.currentTimeMillis();
        
        return new PaymentResult(true, newTransactionId, null);
    }

    private PaymentResult fallbackProcessPayment(Long orderId, BigDecimal amount, Exception e) {
        log.error("Payment failed for orderId={}: {}", orderId, e.getMessage());
        return new PaymentResult(false, null, "Payment service unavailable: " + e.getMessage());
    }

    private PaymentResult fallbackCancelPayment(String transactionId, Exception e) {
        log.error("Payment cancellation failed for transactionId={}: {}", transactionId, e.getMessage());
        return new PaymentResult(false, null, "Payment cancellation failed: " + e.getMessage());
    }
}
