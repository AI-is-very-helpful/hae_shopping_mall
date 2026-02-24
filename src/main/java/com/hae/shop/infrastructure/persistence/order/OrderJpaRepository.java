package com.hae.shop.infrastructure.persistence.order;

import com.hae.shop.infrastructure.persistence.order.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);
}
