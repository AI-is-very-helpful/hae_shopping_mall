package com.hae.shop.interfaces.product.dto;

import java.math.BigDecimal;

/**
 * 상품 생성 요청 DTO
 */
public record CreateProductRequest(
    String name,
    String description,
    BigDecimal price,
    int stockQuantity,
    String category
) {
}
