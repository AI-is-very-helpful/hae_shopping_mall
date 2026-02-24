package com.hae.shop.domain.order.port.out;

import java.math.BigDecimal;

public interface PaymentGatewayPort {

    PaymentResult processPayment(Long orderId, BigDecimal amount);

    PaymentResult cancelPayment(String transactionId);

    record PaymentResult(boolean success, String transactionId, String errorMessage) {
    }
}
