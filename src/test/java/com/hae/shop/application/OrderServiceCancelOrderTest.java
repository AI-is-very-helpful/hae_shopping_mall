package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.port.out.OrderRepositoryPort;
import com.hae.shop.domain.port.out.OutboxPort;
import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import com.hae.shop.domain.product.port.in.ProductService;
import com.hae.shop.domain.coupon.port.in.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceCancelOrderTest {

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

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setMemberId(1L);
        testOrder.setOrderNumber("ORD-12345678");
        testOrder.setStatus(Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 취소 성공 - PENDING 상태")
    void cancelOrder_whenPending_shouldCancel() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.cancelOrder(1L, "테스트 취소 사유");

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(orderRepository).save(any(Order.class));
        verify(outboxPort).saveOrderCancelledEvent(any());
    }

    @Test
    @DisplayName("주문 취소 성공 - PAID 상태")
    void cancelOrder_whenPaid_shouldCancel() {
        testOrder.setStatus(Order.OrderStatus.PAID);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order result = orderService.cancelOrder(1L, "결제 후 취소");

        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(outboxPort).saveOrderCancelledEvent(any());
    }

    @Test
    @DisplayName("주문 취소 실패 - SHIPPED 상태")
    void cancelOrder_whenShipped_shouldThrowInvalidStatus() {
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "취소 불가"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }

    @Test
    @DisplayName("주문 취소 실패 - DELIVERED 상태")
    void cancelOrder_whenDelivered_shouldThrowInvalidStatus() {
        testOrder.setStatus(Order.OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "취소 불가"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }

    @Test
    @DisplayName("주문 취소 실패 - 주문不存在")
    void cancelOrder_whenOrderNotFound_shouldThrow() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(999L, "취소 사유"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }
}
