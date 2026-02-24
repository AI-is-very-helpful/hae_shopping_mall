package com.hae.shop.domain.order.port.in;

import java.math.BigDecimal;

public interface PaymentService {

    boolean processPayment(Long orderId, BigDecimal amount);

    boolean cancelPayment(String transactionId);
}
