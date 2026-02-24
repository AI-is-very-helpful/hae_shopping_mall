package com.hae.shop.domain.product.port.in;

import com.hae.shop.domain.product.model.Product;
import java.util.List;

public interface ProductService {
    Product createProduct(String name, String description, java.math.BigDecimal price, int stockQuantity, String category);
    Product getProduct(Long id);
    List<Product> getProducts(String category);
    void decrementStock(Long productId, int quantity);
}
