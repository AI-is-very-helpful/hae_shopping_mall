package com.hae.shop.application;

import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("결제 성공")
    void processPayment_whenSuccess_shouldReturnTrue() {
        when(paymentGatewayPort.processPayment(any(), any()))
            .thenReturn(new PaymentGatewayPort.PaymentResult(true, "txn-123", null));

        boolean result = paymentService.processPayment(1L, BigDecimal.valueOf(30000));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("결제 실패")
    void processPayment_whenFailure_shouldReturnFalse() {
        when(paymentGatewayPort.processPayment(any(), any()))
            .thenReturn(new PaymentGatewayPort.PaymentResult(false, null, "결제 실패"));

        boolean result = paymentService.processPayment(1L, BigDecimal.valueOf(30000));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("결제 취소 성공")
    void cancelPayment_whenSuccess_shouldReturnTrue() {
        when(paymentGatewayPort.cancelPayment(eq("txn-123")))
            .thenReturn(new PaymentGatewayPort.PaymentResult(true, "txn-123", null));

        boolean result = paymentService.cancelPayment("txn-123");

        assertThat(result).isTrue();
    }
}
