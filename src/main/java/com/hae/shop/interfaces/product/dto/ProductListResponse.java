package com.hae.shop.interfaces.product.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 상품 목록 응답 DTO
 */
public record ProductListResponse(
    List<ProductResponse> products
) {
}
