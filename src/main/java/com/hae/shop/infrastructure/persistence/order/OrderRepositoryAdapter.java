package com.hae.shop.infrastructure.persistence.order;

import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    public Optional<Order> findByIdempotencyKey(String idempotencyKey) {
        return orderJpaRepository.findByIdempotencyKey(idempotencyKey).map(OrderEntity::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = OrderEntity.fromDomain(order);
        OrderEntity saved = orderJpaRepository.save(entity);
        return saved.toDomain();
    }
}
