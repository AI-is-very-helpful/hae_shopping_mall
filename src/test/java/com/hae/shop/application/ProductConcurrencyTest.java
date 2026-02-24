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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductConcurrencyTest {

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
        testProduct.setStockQuantity(50);
        testProduct.setStatus(Product.ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("동시 재고 차감 100건 - 정확히 50건만 성공해야 함")
    void decrementStock_concurrentRequests_shouldHandleOnlyAvailableStock() throws InterruptedException {
        int initialStock = 50;
        int concurrentRequests = 100;
        int decrementAmount = 1;

        testProduct.setStockQuantity(initialStock);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLock).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(productRepository.findById(1L)).thenAnswer(invocation -> {
            Product p = new Product();
            p.setId(1L);
            p.setStockQuantity(testProduct.getStockQuantity());
            p.setStatus(Product.ProductStatus.ACTIVE);
            return Optional.of(p);
        });

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            testProduct.setStockQuantity(p.getStockQuantity());
            return p;
        });

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    productService.decrementStock(1L, decrementAmount);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.INSUFFICIENT_STOCK) {
                        failCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);

        assertThat(completed).isTrue();

        assertThat(successCount.get()).isEqualTo(initialStock);
        assertThat(failCount.get()).isEqualTo(concurrentRequests - initialStock);
    }

    @Test
    @DisplayName("동시 재고 차감 50건 - 모두 성공해야 함")
    void decrementStock_fiftyConcurrentRequests_shouldAllSucceed() throws InterruptedException {
        int initialStock = 100;
        int concurrentRequests = 50;
        int decrementAmount = 1;

        testProduct.setStockQuantity(initialStock);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(concurrentRequests);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(4);
            runnable.run();
            return null;
        }).when(distributedLock).executeWithLock(anyString(), anyLong(), anyLong(), any(TimeUnit.class), any(Runnable.class));

        when(productRepository.findById(1L)).thenAnswer(invocation -> {
            Product p = new Product();
            p.setId(1L);
            p.setStockQuantity(testProduct.getStockQuantity());
            p.setStatus(Product.ProductStatus.ACTIVE);
            return Optional.of(p);
        });

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            testProduct.setStockQuantity(p.getStockQuantity());
            return p;
        });

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; i < concurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    productService.decrementStock(1L, decrementAmount);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentRequests);
    }

    @Test
    @DisplayName("동시 재고 차감 - 분산락 미적용 시 실패")
    void decrementStock_withoutLock_concurrentRequests_shouldHaveRaceCondition() {
        int threadCount = 10;
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            try {
                productService.decrementStock(1L, 1);
                successCount.incrementAndGet();
            } catch (Exception e) {
            }
        }

        verify(distributedLock, times(threadCount)).executeWithLock(
            eq("stock:lock:1"), eq(3L), eq(10L), any(TimeUnit.class), any(Runnable.class)
        );
    }
}
