package com.hae.shop.domain.product.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Product {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity = 0;
    private String category;
    private ProductStatus status = ProductStatus.ACTIVE;
    private Instant createdAt;
    private Instant updatedAt;

    public enum ProductStatus {
        ACTIVE, INACTIVE, OUT_OF_STOCK
    }

    public void decrementStock(int quantity) {
        if (stockQuantity < quantity) {
            throw new RuntimeException("Insufficient stock");
        }
        stockQuantity -= quantity;
        if (stockQuantity == 0) {
            status = ProductStatus.OUT_OF_STOCK;
        }
    }

    public void addStock(int quantity) {
        stockQuantity += quantity;
        if (status == ProductStatus.OUT_OF_STOCK && stockQuantity > 0) {
            status = ProductStatus.ACTIVE;
        }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getCategory() { return category; }
    public ProductStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public void setCategory(String category) { this.category = category; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
