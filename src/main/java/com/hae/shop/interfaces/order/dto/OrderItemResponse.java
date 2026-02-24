package com.hae.shop.interfaces.order.dto;

import com.hae.shop.domain.order.model.OrderItem;

import java.math.BigDecimal;

/**
 * 주문 항목 응답 DTO
 */
public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    Integer quantity,
    BigDecimal price
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getQuantity(),
            item.getProductPrice()
        );
    }
}
