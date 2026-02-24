package com.hae.shop.infrastructure.persistence.order;

import com.hae.shop.domain.order.model.Order;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class OrderRepositoryAdapterTest {

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
    private OrderRepositoryAdapter orderRepositoryAdapter;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderJpaRepository.deleteAll();

        testOrder = new Order();
        testOrder.setMemberId(1L);
        testOrder.setOrderNumber("ORD-12345678");
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(30000));
    }

    @Test
    @DisplayName("주문 저장 성공")
    void save_shouldPersistOrder() {
        Order saved = orderRepositoryAdapter.save(testOrder);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isEqualTo("ORD-12345678");
    }

    @Test
    @DisplayName("ID로 주문 조회 성공")
    void findById_whenExists_shouldReturnOrder() {
        Order saved = orderRepositoryAdapter.save(testOrder);

        Optional<Order> found = orderRepositoryAdapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo("ORD-12345678");
    }

    @Test
    @DisplayName("멱등성 키로 주문 조회")
    void findByIdempotencyKey_whenExists_shouldReturnOrder() {
        testOrder.setIdempotencyKey("unique-key-123");
        Order saved = orderRepositoryAdapter.save(testOrder);

        Optional<Order> found = orderRepositoryAdapter.findByIdempotencyKey("unique-key-123");

        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo("unique-key-123");
    }

    @Test
    @DisplayName("주문 상태 업데이트 - PENDING에서 PAID로")
    void save_withStatusChange_shouldPersistNewStatus() {
        Order saved = orderRepositoryAdapter.save(testOrder);

        saved.setStatus(Order.OrderStatus.PAID);
        Order updated = orderRepositoryAdapter.save(saved);

        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.PAID);

        Optional<Order> found = orderRepositoryAdapter.findById(saved.getId());
        assertThat(found.get().getStatus()).isEqualTo(Order.OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문 상태 업데이트 - PAID에서 CANCELLED로")
    void save_withCancel_shouldPersistCancelledStatus() {
        testOrder.setStatus(Order.OrderStatus.PAID);
        Order saved = orderRepositoryAdapter.save(testOrder);

        saved.setStatus(Order.OrderStatus.CANCELLED);
        Order updated = orderRepositoryAdapter.save(saved);

        assertThat(updated.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
    }
}
