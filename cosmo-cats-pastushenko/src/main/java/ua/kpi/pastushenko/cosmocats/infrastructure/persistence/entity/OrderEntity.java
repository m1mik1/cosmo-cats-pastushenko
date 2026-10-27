package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import ua.kpi.pastushenko.cosmocats.domain.model.Order;
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_id")
    @SequenceGenerator(name = "order_id", sequenceName = "orders_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") private List<OrderItemEntity> items = new ArrayList<>();
    protected OrderEntity() {}
    public OrderEntity(Order value, LongFunction<ProductEntity> products) {
        id = value.getId(); createdAt = value.getCreatedAt(); replaceItems(value, products);
    }
    public void replaceItems(Order value, LongFunction<ProductEntity> products) {
        items.clear();
        for (var item : value.getItems()) { items.add(new OrderItemEntity(this, products.apply(item.getProductId()), item)); }
    }
    public Order toDomain() { return new Order(id, items.stream().map(OrderItemEntity::toDomain).toList(), createdAt); }
}
