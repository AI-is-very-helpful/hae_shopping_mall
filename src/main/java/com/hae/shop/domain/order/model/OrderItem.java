package com.hae.shop.domain.order.model;

import java.math.BigDecimal;

public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer quantity;
    private BigDecimal subtotal;

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getProductPrice() { return productPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }

    public void setId(Long id) { this.id = id; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setProductPrice(BigDecimal productPrice) { this.productPrice = productPrice; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
