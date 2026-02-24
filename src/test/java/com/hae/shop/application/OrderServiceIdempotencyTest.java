package com.hae.shop.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.port.out.OrderRepositoryPort;
import com.hae.shop.domain.order.port.out.OutboxPort;
import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import com.hae.shop.domain.product.port.in.ProductService;
import com.hae.shop.domain.coupon.port.in.CouponService;

class OrderServiceIdempotencyTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private ProductService productService;

    @Mock
    private CouponService couponService;

    @Mock
    private PaymentGatewayPort paymentGatewayPort;

    @Mock
    private OutboxPort outboxPort;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void payOrder_withSameIdempotencyKey_shouldReturnExistingPaidOrder() {
        // Given
        Long orderId = 1L;
        String paymentToken = "token123";
        String idempotencyKey = "key123";

        Order existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setStatus(Order.OrderStatus.PAID);
        existingOrder.setIdempotencyKey(idempotencyKey);
        existingOrder.setPaymentAmount(BigDecimal.valueOf(100));
        existingOrder.setMemberId(1L);
        existingOrder.setOrderNumber("ORD-123");
        existingOrder.setTotalAmount(BigDecimal.valueOf(100));
        existingOrder.setDiscountAmount(BigDecimal.ZERO);
        existingOrder.setItems(new ArrayList<>());

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existingOrder));

        // When
        Order result = orderService.payOrder(orderId, paymentToken, idempotencyKey);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        verify(paymentGatewayPort, never()).processPayment(any(), any());
        verify(outboxPort, never()).savePaymentCompletedEvent(any());
    }

    @Test
    void payOrder_withIdempotencyKey_notPaidYet_shouldProcessPaymentAndSetKey() {
        // Given
        Long orderId = 1L;
        String paymentToken = "token123";
        String idempotencyKey = "key123";

        Order order = new Order();
        order.setId(orderId);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setIdempotencyKey(null);
        order.setPaymentAmount(BigDecimal.valueOf(100));
        order.setMemberId(1L);
        order.setOrderNumber("ORD-123");
        order.setTotalAmount(BigDecimal.valueOf(100));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setItems(new ArrayList<>());

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentGatewayPort.processPayment(orderId, BigDecimal.valueOf(100)))
            .thenReturn(new PaymentGatewayPort.PaymentResult(true, "tx123", null));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Order result = orderService.payOrder(orderId, paymentToken, idempotencyKey);

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(result.getIdempotencyKey()).isEqualTo(idempotencyKey);
        verify(paymentGatewayPort).processPayment(orderId, BigDecimal.valueOf(100));
        verify(outboxPort).savePaymentCompletedEvent(any());
    }

    @Test
    void payOrder_withDuplicateIdempotencyKeyOnDifferentOrder_shouldThrowConflict() {
        // Given
        Long orderId = 1L;
        String paymentToken = "token123";
        String idempotencyKey = "key123";

        Order otherOrder = new Order();
        otherOrder.setId(2L);
        otherOrder.setStatus(Order.OrderStatus.PAID);
        otherOrder.setIdempotencyKey(idempotencyKey);

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(otherOrder));

        // When/Then
        assertThatThrownBy(() -> orderService.payOrder(orderId, paymentToken, idempotencyKey))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }
}
