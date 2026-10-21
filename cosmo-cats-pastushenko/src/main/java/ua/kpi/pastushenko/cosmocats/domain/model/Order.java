package ua.kpi.pastushenko.cosmocats.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class Order {

    private final Long id;
    private final List<OrderItem> items;
    private final Instant createdAt;

    public Order(Long id, List<OrderItem> items, Instant createdAt) {
        DomainChecks.optionalId(id);
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        this.id = id;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
}
