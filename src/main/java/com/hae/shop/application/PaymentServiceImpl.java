package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
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

        if (!result.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        return true;
    }

    @Override
    @Transactional
    public boolean cancelPayment(String transactionId) {
        PaymentGatewayPort.PaymentResult result = paymentGatewayPort.cancelPayment(transactionId);

        if (!result.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }

        return true;
    }
}
