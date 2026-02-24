package com.hae.shop.domain.order.port.out;

import com.hae.shop.domain.order.model.Order;
import java.util.Optional;

public interface OrderRepositoryPort {
    Optional<Order> findById(Long id);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Order save(Order order);
}
