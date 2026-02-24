package com.hae.shop.interfaces.order.dto;

/**
 * 주문 항목 추가 요청 DTO
 */
public record AddOrderItemRequest(
    Long productId,
    Integer quantity
) {
}
