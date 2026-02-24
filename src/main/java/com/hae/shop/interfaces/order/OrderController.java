package com.hae.shop.interfaces.order;

import com.hae.shop.domain.order.port.in.OrderService;
import com.hae.shop.interfaces.order.dto.CreateOrderRequest;
import com.hae.shop.interfaces.order.dto.OrderResponse;
import com.hae.shop.interfaces.order.dto.AddOrderItemRequest;
import com.hae.shop.interfaces.order.dto.ApplyCouponRequest;
import com.hae.shop.interfaces.order.dto.PayOrderRequest;
import com.hae.shop.interfaces.order.dto.CancelOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request.memberId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        var order = orderService.getOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(@PathVariable Long id, @Valid @RequestBody AddOrderItemRequest request) {
        var order = orderService.addItem(id, request.productId(), request.quantity());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/coupons")
    public ResponseEntity<OrderResponse> applyCoupon(@PathVariable Long id, @Valid @RequestBody ApplyCouponRequest request) {
        var order = orderService.applyCoupon(id, request.couponId());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> payOrder(@PathVariable Long id, @Valid @RequestBody PayOrderRequest request) {
        var order = orderService.payOrder(id, request.paymentToken());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        var order = orderService.cancelOrder(id, request.reason());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
