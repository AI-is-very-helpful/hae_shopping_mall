package com.hae.shop.domain.product.port.out;

import com.hae.shop.domain.product.model.Product;
import java.util.Optional;

public interface ProductRepositoryPort {
    Optional<Product> findById(Long id);
    Product save(Product product);
    java.util.List<Product> findByCategory(String category);
    java.util.List<Product> findAll();
}
