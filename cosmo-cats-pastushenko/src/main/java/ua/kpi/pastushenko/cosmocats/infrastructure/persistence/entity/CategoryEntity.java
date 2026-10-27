package ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity;
import jakarta.persistence.*;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
@Entity
@Table(name = "categories")
public class CategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "category_id")
    @SequenceGenerator(name = "category_id", sequenceName = "categories_id_seq", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 120) private String name;
    protected CategoryEntity() {}
    public CategoryEntity(Category value) { id = value.getId(); name = value.getName(); }
    public Long getId() { return id; }
    public void rename(String name) { this.name = name; }
    public Category toDomain() { return new Category(id, name); }
}
