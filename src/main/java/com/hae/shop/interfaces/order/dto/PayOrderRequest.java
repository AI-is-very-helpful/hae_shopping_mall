package com.hae.shop.interfaces.order.dto;

/**
 * 주문 결제 요청 DTO
 */
public record PayOrderRequest(
    String paymentToken
) {
}
