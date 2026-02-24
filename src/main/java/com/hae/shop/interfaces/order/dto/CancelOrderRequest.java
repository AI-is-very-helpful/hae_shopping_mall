package com.hae.shop.interfaces.order.dto;

/**
 * 주문 취소 요청 DTO
 */
public record CancelOrderRequest(
    String reason
) {
}
