package com.hae.shop.infrastructure.persistence.product;

import com.hae.shop.domain.product.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class ProductRepositoryAdapterTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepositoryAdapter productRepositoryAdapter;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productJpaRepository.deleteAll();

        testProduct = new Product();
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(BigDecimal.valueOf(10000));
        testProduct.setStockQuantity(50);
        testProduct.setCategory("ELECTRONICS");
        testProduct.setStatus(Product.ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("상품 저장 성공")
    void save_shouldPersistProduct() {
        Product saved = productRepositoryAdapter.save(testProduct);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("ID로 상품 조회 성공")
    void findById_whenExists_shouldReturnProduct() {
        Product saved = productRepositoryAdapter.save(testProduct);

        Optional<Product> found = productRepositoryAdapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("ID로 상품 조회 실패 - 존재하지 않음")
    void findById_whenNotExists_shouldReturnEmpty() {
        Optional<Product> found = productRepositoryAdapter.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("카테고리로 상품 조회")
    void findByCategory_shouldReturnProducts() {
        productRepositoryAdapter.save(testProduct);

        Product product2 = new Product();
        product2.setName("Product 2");
        product2.setPrice(BigDecimal.valueOf(20000));
        product2.setStockQuantity(30);
        product2.setCategory("ELECTRONICS");
        product2.setStatus(Product.ProductStatus.ACTIVE);
        productRepositoryAdapter.save(product2);

        List<Product> electronics = productRepositoryAdapter.findByCategory("ELECTRONICS");

        assertThat(electronics).hasSize(2);
    }

    @Test
    @DisplayName("재고 차감 - Persistence Layer")
    void save_withStockDecrement_shouldPersistUpdatedStock() {
        Product saved = productRepositoryAdapter.save(testProduct);

        saved.setStockQuantity(45);
        Product updated = productRepositoryAdapter.save(saved);

        assertThat(updated.getStockQuantity()).isEqualTo(45);

        Optional<Product> found = productRepositoryAdapter.findById(saved.getId());
        assertThat(found.get().getStockQuantity()).isEqualTo(45);
    }
}
