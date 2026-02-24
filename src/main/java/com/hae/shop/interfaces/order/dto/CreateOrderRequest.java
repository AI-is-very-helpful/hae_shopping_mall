package com.hae.shop.interfaces.order.dto;

/**
 * 주문 생성 요청 DTO
 */
public record CreateOrderRequest(
    Long memberId
) {
}
