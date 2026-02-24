package com.hae.shop.infrastructure.persistence.order;

import com.hae.shop.domain.order.model.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * JPA 엔티티: OrderItem 도메인 값 객체의 영구 저장 표현.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    /**
     * 항목의 순서 (선택적).
     */
    @Column(name = "item_order")
    private Integer itemOrder;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    /**
     * 도메인 OrderItem에서 OrderItemEntity로 변환.
     */
    public static OrderItemEntity fromDomain(OrderItem item) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.id = item.getId();
        entity.productId = item.getProductId();
        entity.productName = item.getProductName();
        entity.productPrice = item.getProductPrice();
        entity.quantity = item.getQuantity();
        entity.subtotal = item.getSubtotal();
        return entity;
    }

    /**
     * OrderItemEntity를 도메인 OrderItem로 변환.
     */
    public OrderItem toDomain() {
        OrderItem item = new OrderItem();
        item.setId(this.id);
        item.setProductId(this.productId);
        item.setProductName(this.productName);
        item.setProductPrice(this.productPrice);
        item.setQuantity(this.quantity);
        item.setSubtotal(this.subtotal);
        return item;
    }
}
