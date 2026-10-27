package ua.kpi.pastushenko.cosmocats.infrastructure.repository;

import ua.kpi.pastushenko.cosmocats.domain.model.PageRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.domain.model.ProductPage;
import ua.kpi.pastushenko.cosmocats.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class InMemoryProductRepository implements ProductRepository {

    private final NavigableMap<Long, Product> products = new TreeMap<>();
    private long nextId = 1;

    public InMemoryProductRepository() {
        create(new Product(null, "Cosmic Milk", "Milk for space cats",
                new BigDecimal("12.50"), 1, 20));
        create(new Product(null, "Galaxy Yarn", "An antigravity ball of yarn",
                new BigDecimal("8.99"), 2, 15));
        create(new Product(null, "Comet Collar", "A glowing collar for space travel",
                new BigDecimal("25.00"), 3, 10));
    }

    @Override
    public synchronized Product create(Product product) {
        Objects.requireNonNull(product, "product must not be null");
        if (product.getId() != null) {
            throw new IllegalArgumentException("new product must not have an id");
        }
        Product stored = product.withId(nextId++);
        products.put(stored.getId(), stored);
        return stored;
    }

    @Override
    public synchronized Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public synchronized ProductPage findPage(PageRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var content = products.values().stream()
                .skip(request.offset())
                .limit(request.size())
                .toList();
        return new ProductPage(content, request.page(), request.size(), products.size());
    }

    @Override
    public synchronized Optional<Product> update(long id, Product product) {
        Objects.requireNonNull(product, "product must not be null");
        if (!products.containsKey(id)) {
            return Optional.empty();
        }
        Product updated = product.withId(id);
        products.put(id, updated);
        return Optional.of(updated);
    }

    @Override
    public synchronized boolean deleteById(long id) {
        return products.remove(id) != null;
    }
}
