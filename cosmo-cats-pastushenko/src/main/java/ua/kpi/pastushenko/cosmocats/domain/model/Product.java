package ua.kpi.pastushenko.cosmocats.domain.model;

import java.math.BigDecimal;

public final class Product {

    private final Long id;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final long categoryId;
    private final int stockQuantity;

    public Product(Long id, String name, String description, BigDecimal price,
                   long categoryId, int stockQuantity) {
        DomainChecks.optionalId(id);
        DomainChecks.name(name, 3);
        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException("description must not exceed 2000 characters");
        }
        DomainChecks.positiveId(categoryId, "categoryId");
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("stockQuantity must not be negative");
        }
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = DomainChecks.price(price);
        this.categoryId = categoryId;
        this.stockQuantity = stockQuantity;
    }

    public Product withId(long id) {
        return new Product(id, name, description, price, categoryId, stockQuantity);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }
}
