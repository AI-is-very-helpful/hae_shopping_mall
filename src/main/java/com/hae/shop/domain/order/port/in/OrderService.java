package com.hae.shop.domain.order.port.in;

import com.hae.shop.domain.order.model.Order;

public interface OrderService {
    Order createOrder(Long memberId, String idempotencyKey);
    Order getOrder(Long orderId);
    void completePayment(Long orderId);
    Order addItem(Long orderId, Long productId, int quantity);
    Order applyCoupon(Long orderId, Long couponId);
    Order payOrder(Long orderId, String paymentToken, String idempotencyKey);
    Order cancelOrder(Long orderId, String reason);
}
