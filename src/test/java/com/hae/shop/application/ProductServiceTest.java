package com.hae.shop.application;

import com.hae.shop.common.BusinessException;
import com.hae.shop.common.ErrorCode;
import com.hae.shop.config.DistributedLock;
import com.hae.shop.domain.product.model.Product;
import com.hae.shop.domain.product.port.out.ProductRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private DistributedLock distributedLock;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.valueOf(10000));
        testProduct.setStockQuantity(10);
        testProduct.setStatus(Product.ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("상품 생성 성공")
    void createProduct_shouldReturnProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        Product result = productService.createProduct(
            "Test Product", "Description", BigDecimal.valueOf(10000), 10, "ELECTRONICS"
        );

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 조회 성공")
    void getProduct_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        Product result = productService.getProduct(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("상품 조회 실패 - 존재하지 않음")
    void getProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decrementStock_shouldSucceed() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLock).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.decrementStock(1L, 5);

        verify(distributedLock).executeWithLock(
            eq("stock:lock:1"), eq(3L), eq(10L), any(TimeUnit.class), any(Runnable.class)
        );
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("재고 차감 실패 - 재고 부족")
    void decrementStock_shouldThrowWhenInsufficientStock() {
        testProduct.setStockQuantity(3);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLock).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.decrementStock(1L, 5))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INSUFFICIENT_STOCK));
    }

    @Test
    @DisplayName("재고 차감 실패 - 상품 비활성")
    void decrementStock_shouldThrowWhenProductNotActive() {
        testProduct.setStatus(Product.ProductStatus.INACTIVE);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLock).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        assertThatThrownBy(() -> productService.decrementStock(1L, 1))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_ACTIVE));
    }
}
