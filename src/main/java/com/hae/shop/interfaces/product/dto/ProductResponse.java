package com.hae.shop.interfaces.product.dto;

import com.hae.shop.domain.product.model.Product;

import java.math.BigDecimal;

/**
 * 상품 응답 DTO (단일 상품)
 */
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stockQuantity,
    String category,
    String status
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getCategory(),
            product.getStatus().name()
        );
    }
}
