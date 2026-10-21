package ua.kpi.pastushenko.cosmocats.api.mapper;

import ua.kpi.pastushenko.cosmocats.api.dto.ProductResponse;
import ua.kpi.pastushenko.cosmocats.api.dto.ProductWriteRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;

import java.util.Objects;

public final class ProductMapper {

    public Product toDomain(ProductWriteRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        if (request.categoryId() == null || request.stockQuantity() == null) {
            throw new IllegalArgumentException("categoryId and stockQuantity are required");
        }
        return new Product(
                null,
                request.name(),
                request.description(),
                request.price(),
                request.categoryId(),
                request.stockQuantity()
        );
    }

    public ProductResponse toDto(Product product) {
        Objects.requireNonNull(product, "product must not be null");
        if (product.getId() == null) {
            throw new IllegalArgumentException("product must have an id before it is returned by the API");
        }
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategoryId(),
                product.getStockQuantity()
        );
    }
}
