package ua.kpi.pastushenko.cosmocats.domain.model;

import java.util.List;

public final class Cart {

    private final Long id;
    private final List<CartItem> items;

    public Cart(Long id, List<CartItem> items) {
        DomainChecks.optionalId(id);
        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }
        this.id = id;
        this.items = List.copyOf(items);
    }

    public Long getId() {
        return id;
    }

    public List<CartItem> getItems() {
        return items;
    }
}
