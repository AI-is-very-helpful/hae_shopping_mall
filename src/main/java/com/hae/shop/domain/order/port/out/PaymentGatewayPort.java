package com.hae.shop.domain.order.port.out;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    @CircuitBreaker(name = "paymentGateway")
    @Retry(name = "paymentGateway")
    PaymentResult processPayment(Long orderId, BigDecimal amount);

    @CircuitBreaker(name = "paymentGateway")
    @Retry(name = "paymentGateway")
    PaymentResult cancelPayment(String transactionId);

    record PaymentResult(boolean success, String transactionId, String errorMessage) {
    }
}
