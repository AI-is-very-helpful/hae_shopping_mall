package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.domain.order.model.Order;
import com.hae.shop.domain.order.model.OrderCreatedEvent;
import com.hae.shop.domain.order.model.OrderItem;
import com.hae.shop.domain.order.model.PaymentCompletedEvent;
import com.hae.shop.domain.order.model.OrderCancelledEvent;
import com.hae.shop.domain.order.port.in.OrderService;
import com.hae.shop.domain.order.port.out.OrderRepositoryPort;
import com.hae.shop.domain.port.out.OutboxPort;
import com.hae.shop.domain.product.model.Product;
import com.hae.shop.domain.product.port.in.ProductService;
import com.hae.shop.domain.coupon.port.in.CouponService;
import com.hae.shop.domain.order.port.out.PaymentGatewayPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepositoryPort orderRepository;
    private final ProductService productService;
    private final CouponService couponService;
    private final PaymentGatewayPort paymentGatewayPort;
    private final OutboxPort outboxPort;

    @Override
    @Transactional
    public Order createOrder(Long memberId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
        }

        Order order = new Order();
        order.setMemberId(memberId);
        order.setOrderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPaymentAmount(BigDecimal.ZERO);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        order.setItems(new ArrayList<>());

        Order savedOrder = orderRepository.save(order);
        
        outboxPort.saveOrderCreatedEvent(OrderCreatedEvent.from(savedOrder));
        
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void completePayment(Long orderId) {
        Order order = getOrder(orderId);
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order addItem(Long orderId, Long productId, int quantity) {
        Order order = getOrder(orderId);
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        
        productService.decrementStock(productId, quantity);
        
        Product product = productService.getProduct(productId);
        
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(product.getName());
        item.setProductPrice(product.getPrice());
        item.setQuantity(quantity);
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        
        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }
        order.getItems().add(item);
        
        recalculateTotals(order);
        
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order applyCoupon(Long orderId, Long couponId) {
        Order order = getOrder(orderId);
        
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        
        BigDecimal discount = couponService.applyDiscount(couponId, order.getTotalAmount());
        
        order.setDiscountAmount(discount);
        
        recalculateTotals(order);
        
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order payOrder(Long orderId, String paymentToken, String idempotencyKey) {
        // Idempotency check: if key provided, see if already processed
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existingOrder.isPresent()) {
                Order found = existingOrder.get();
                if (found.getId().equals(orderId)) {
                    // Retry of same payment - return the existing paid order
                    if (found.getStatus() == Order.OrderStatus.PAID) {
                        return found;
                    }
                    // If found but not paid, continue to process (shouldn't happen normally)
                } else {
                    // Key belongs to a different order - conflict
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
                }
            }
        }

        Order order = getOrder(orderId);
        if (order.getStatus() == Order.OrderStatus.PAID) {
            return order;
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        var paymentResult = paymentGatewayPort.processPayment(
            orderId,
            order.getPaymentAmount()
        );
        if (!paymentResult.success()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
        
        order.setStatus(Order.OrderStatus.PAID);
        
        // Set idempotencyKey if provided and not already set
        if (idempotencyKey != null && !idempotencyKey.isEmpty() && order.getIdempotencyKey() == null) {
            order.setIdempotencyKey(idempotencyKey);
        }
        
        Order savedOrder = orderRepository.save(order);
        outboxPort.savePaymentCompletedEvent(
            PaymentCompletedEvent.from(savedOrder, "CARD", "receipt-" + paymentResult.transactionId())
        );
        return savedOrder;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, String reason) {
        Order order = getOrder(orderId);
        
        if (order.getStatus() != Order.OrderStatus.PENDING && 
            order.getStatus() != Order.OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        
        outboxPort.saveOrderCancelledEvent(
            OrderCancelledEvent.from(savedOrder, reason)
        );
        
        return savedOrder;
    }
    
    private void recalculateTotals(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                total = total.add(item.getSubtotal());
            }
        }
        order.setTotalAmount(total);
        
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        order.setPaymentAmount(total.subtract(discount));
    }
}
