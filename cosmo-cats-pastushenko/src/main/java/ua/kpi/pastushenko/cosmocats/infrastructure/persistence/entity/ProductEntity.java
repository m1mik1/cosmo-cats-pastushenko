package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
@Entity
@Table(name = "products")
public class ProductEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_id")
    @SequenceGenerator(name = "product_id", sequenceName = "products_id_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 2000) private String description;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false) private CategoryEntity category;
    @Column(name = "stock_quantity", nullable = false) private int stockQuantity;
    protected ProductEntity() {}
    public ProductEntity(Product value, CategoryEntity category) { id = value.getId(); replace(value, category); }
    public Long getId() { return id; }
    public void replace(Product value, CategoryEntity category) {
        name = value.getName(); description = value.getDescription(); price = value.getPrice();
        this.category = category; stockQuantity = value.getStockQuantity();
    }
    public Product toDomain() { return new Product(id, name, description, price, category.getId(), stockQuantity); }
}
