package ua.kpi.pastushenko.cosmocats.infrastructure.persistence;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.ProductRepository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.ProductEntity;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public class JpaProductRepository implements ProductRepository {
    private final SpringDataProductRepository data;
    private final SpringDataCategoryRepository references;
    public JpaProductRepository(SpringDataProductRepository data, SpringDataCategoryRepository references) { this.data = data; this.references = references; }
    public Product create(Product value) { return data.saveAndFlush(new ProductEntity(value, references.getReferenceById(value.getCategoryId()))).toDomain(); }
    public Optional<Product> findById(long id) { return data.findById(id).map(ProductEntity::toDomain); }
    public ProductPage findPage(PageRequest request) {
        // JPA uses an int offset. Preserve the API's long arithmetic for large pages.
        if (request.offset() > Integer.MAX_VALUE) { return new ProductPage(List.of(), request.page(), request.size(), data.count()); }
        var page = data.findAll(org.springframework.data.domain.PageRequest.of(request.page(), request.size(), Sort.by("id")));
        return new ProductPage(page.getContent().stream().map(ProductEntity::toDomain).toList(), request.page(), request.size(), page.getTotalElements());
    }
    public Optional<Product> update(long id, Product value) {
        return data.findForUpdate(id).map(entity -> { entity.replace(value, references.getReferenceById(value.getCategoryId())); return data.saveAndFlush(entity).toDomain(); });
    }
    public boolean deleteById(long id) {
        var entity = data.findForUpdate(id);
        if (entity.isEmpty()) { return false; }
        data.delete(entity.get()); data.flush(); return true;
    }

}
