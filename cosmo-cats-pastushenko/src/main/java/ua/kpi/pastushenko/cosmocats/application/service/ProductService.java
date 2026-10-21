package ua.kpi.pastushenko.cosmocats.application.service;

import org.springframework.stereotype.Service;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.domain.model.PageRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.domain.model.ProductPage;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;
import ua.kpi.pastushenko.cosmocats.domain.repository.ProductRepository;

import java.util.Objects;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductPage findPage(int page, int size) {
        return productRepository.findPage(new PageRequest(page, size));
    }

    public Product getById(long id) {
        requirePositiveId(id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Product create(Product product) {
        Objects.requireNonNull(product, "product must not be null");
        if (product.getId() != null) {
            throw new IllegalArgumentException("new product must not have an id");
        }
        requireCategory(product.getCategoryId());
        return productRepository.create(product);
    }

    public Product update(long id, Product product) {
        requirePositiveId(id);
        Objects.requireNonNull(product, "product must not be null");
        if (product.getId() != null && product.getId() != id) {
            throw new IllegalArgumentException("product id must match the requested id");
        }
        requireCategory(product.getCategoryId());
        return productRepository.update(id, product)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public void delete(long id) {
        requirePositiveId(id);
        if (!productRepository.deleteById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
    }

    private void requireCategory(long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
    }

    private void requirePositiveId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("productId must be positive");
        }
    }
}
