package com.hae.shop.application;

import com.hae.shop.domain.order.port.in.PaymentService;
import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentGatewayPort paymentGatewayPort;

    @Override
    @Transactional
    public boolean processPayment(Long orderId, BigDecimal amount) {
        PaymentGatewayPort.PaymentResult result = paymentGatewayPort.processPayment(orderId, amount);
        return result.success();
    }

    @Override
    @Transactional
    public boolean cancelPayment(String transactionId) {
        PaymentGatewayPort.PaymentResult result = paymentGatewayPort.cancelPayment(transactionId);
        return result.success();
    }
}
