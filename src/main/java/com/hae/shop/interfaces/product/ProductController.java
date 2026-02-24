package com.hae.shop.interfaces.product;

import com.hae.shop.domain.product.port.in.ProductService;
import com.hae.shop.interfaces.product.dto.CreateProductRequest;
import com.hae.shop.interfaces.product.dto.ProductListResponse;
import com.hae.shop.interfaces.product.dto.ProductResponse;
import com.hae.shop.domain.product.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product product = productService.createProduct(
            request.name(), request.description(), request.price(), request.stockQuantity(), request.category()
        );
        ProductResponse response = ProductResponse.from(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ProductListResponse> getProducts(@RequestParam(required = false) String category) {
        List<Product> products = productService.getProducts(category);
        List<ProductResponse> responseList = products.stream()
            .map(ProductResponse::from)
            .toList();
        return ResponseEntity.ok(new ProductListResponse(responseList));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Product product = productService.getProduct(id);
        return ResponseEntity.ok(ProductResponse.from(product));
    }
}
