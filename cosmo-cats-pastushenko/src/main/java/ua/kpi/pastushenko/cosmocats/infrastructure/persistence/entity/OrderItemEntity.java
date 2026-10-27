package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import ua.kpi.pastushenko.cosmocats.domain.model.OrderItem;
@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_id")
    @SequenceGenerator(name = "order_item_id", sequenceName = "order_items_id_seq", allocationSize = 1)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false) private ProductEntity product;
    @Column(name = "product_name", nullable = false, length = 120) private String productName;
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity;
    protected OrderItemEntity() {}
    public OrderItemEntity(OrderEntity order, ProductEntity product, OrderItem item) {
        this.order = order; this.product = product; productName = item.getProductName();
        unitPrice = item.getUnitPrice(); quantity = item.getQuantity();
    }
    public OrderItem toDomain() { return new OrderItem(product.getId(), productName, unitPrice, quantity); }
}
