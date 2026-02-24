package com.hae.shop.interfaces.order;

import com.hae.shop.domain.order.port.in.OrderService;
import com.hae.shop.interfaces.order.dto.CreateOrderRequest;
import com.hae.shop.interfaces.order.dto.OrderResponse;
import com.hae.shop.interfaces.order.dto.AddOrderItemRequest;
import com.hae.shop.interfaces.order.dto.ApplyCouponRequest;
import com.hae.shop.interfaces.order.dto.PayOrderRequest;
import com.hae.shop.interfaces.order.dto.CancelOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "주문 관리 API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "새 주문을 생성합니다")
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "멱등성 키", required = false) @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {
        var order = orderService.createOrder(request.memberId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{id}")
    @Operation(summary = "주문 조회", description = "주문 ID로 상세 정보를 조회합니다")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        var order = orderService.getOrder(id);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "주문 상품 추가", description = "주문에 상품을 추가합니다")
    public ResponseEntity<OrderResponse> addItem(@PathVariable Long id, @Valid @RequestBody AddOrderItemRequest request) {
        var order = orderService.addItem(id, request.productId(), request.quantity());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/coupons")
    @Operation(summary = "쿠폰 적용", description = "주문에 쿠폰을 적용합니다")
    public ResponseEntity<OrderResponse> applyCoupon(@PathVariable Long id, @Valid @RequestBody ApplyCouponRequest request) {
        var order = orderService.applyCoupon(id, request.couponId());
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "주문 결제", description = "주문 결제를 수행합니다")
    public ResponseEntity<OrderResponse> payOrder(
            @PathVariable Long id,
            @Parameter(description = "멱등성 키", required = false) @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PayOrderRequest request) {
        var order = orderService.payOrder(id, request.paymentToken(), idempotencyKey);
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "주문 취소", description = "주문을 취소합니다")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id, @Valid @RequestBody CancelOrderRequest request) {
        var order = orderService.cancelOrder(id, request.reason());
        return ResponseEntity.ok(OrderResponse.from(order));
    }
}
