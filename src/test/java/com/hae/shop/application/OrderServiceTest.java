package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.port.out.OrderRepositoryPort;
import com.hae.shop.domain.order.port.out.OutboxPort;
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
class OrderServiceTest {

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
    @DisplayName("주문 생성 성공")
    void createOrder_shouldReturnOrder() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        Order result = orderService.createOrder(1L, null);

        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 생성 성공 - 멱등성 키 제공")
    void createOrder_shouldCheckIdempotencyKey() {
        String idempotencyKey = "unique-key-123";
        
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            return order;
        });

        Order result = orderService.createOrder(1L, idempotencyKey);

        assertThat(result).isNotNull();
        verify(orderRepository).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("주문 생성 실패 - 중복 멱등성 키")
    void createOrder_shouldThrowWhenDuplicateIdempotencyKey() {
        String idempotencyKey = "duplicate-key";
        
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.createOrder(1L, idempotencyKey))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT));
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_shouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        Order result = orderService.getOrder(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("주문 조회 실패 - 존재하지 않음")
    void getOrder_shouldThrowWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_NOT_FOUND));
    }

    @Test
    @DisplayName("결제 완료 성공")
    void completePayment_shouldSucceed() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        orderService.completePayment(1L);

        assertThat(testOrder.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("결제 완료 실패 - 잘못된 주문 상태")
    void completePayment_shouldThrowWhenInvalidStatus() {
        testOrder.setStatus(Order.OrderStatus.PAID);
        
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.completePayment(1L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_STATUS));
    }
}
