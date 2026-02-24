package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.config.DistributedLock;
import com.hae.shop.domain.product.model.Product;
import com.hae.shop.domain.product.port.in.ProductService;
import com.hae.shop.domain.product.port.out.ProductRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String LOCK_PREFIX = "stock:lock:";
    private static final long LOCK_WAIT_TIME = 3L;
    private static final long LOCK_LEASE_TIME = 10L;

    private final ProductRepositoryPort productRepository;
    private final DistributedLock distributedLock;

    @Override
    @Transactional
    public Product createProduct(String name, String description, java.math.BigDecimal price, int stockQuantity, String category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        product.setStatus(Product.ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "#id")
    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories", key = "#category")
    public List<Product> getProducts(String category) {
        if (category != null && !category.isEmpty()) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Override
    @Transactional
    public void decrementStock(Long productId, int quantity) {
        String lockKey = LOCK_PREFIX + productId;
        
        distributedLock.executeWithLock(
            lockKey,
            LOCK_WAIT_TIME,
            LOCK_LEASE_TIME,
            TimeUnit.SECONDS,
            () -> doDecrementStock(productId, quantity)
        );
    }

    private void doDecrementStock(Long productId, int quantity) {
        Product product = getProduct(productId);
        
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }
        
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
        product.decrementStock(quantity);
        productRepository.save(product);
    }
}
