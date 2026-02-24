package com.hae.shop.infrastructure.persistence.product;

import com.hae.shop.domain.product.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(String category);
}
