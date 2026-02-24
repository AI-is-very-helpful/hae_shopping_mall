package com.hae.shop.interfaces.order.dto;

import java.math.BigDecimal;
import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.model.OrderItem;

import java.time.Instant;
import java.util.List;

/**
 * 주문 응답 DTO
 */
public record OrderResponse(
    Long id,
    String orderNumber,
    Long memberId,
    BigDecimal totalAmount,
    BigDecimal discountAmount,
    BigDecimal paymentAmount,
    String status,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems() != null
            ? order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList()
            : List.of();
        
        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getMemberId(),
            order.getTotalAmount(),
            order.getDiscountAmount(),
            order.getPaymentAmount(),
            order.getStatus().name(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
