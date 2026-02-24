package com.hae.shop.infrastructure.persistence.product;

import com.hae.shop.domain.product.model.Product;
import com.hae.shop.domain.product.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public List<Product> findByCategory(String category) {
        return productJpaRepository.findByCategory(category);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll();
    }
}
