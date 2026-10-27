package ua.kpi.pastushenko.cosmocats.infrastructure.persistence;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.CategoryEntity;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public class JpaCategoryRepository implements CategoryRepository {
    private final SpringDataCategoryRepository data;

    public JpaCategoryRepository(SpringDataCategoryRepository data) { this.data = data;  }
    public Category create(Category value) { return data.saveAndFlush(new CategoryEntity(value)).toDomain(); }
    public Optional<Category> findById(long id) { return data.findById(id).map(CategoryEntity::toDomain); }
    public PageResult<Category> findPage(PageRequest request) {
        // JPA uses an int offset. Preserve the API's long arithmetic for large pages.
        if (request.offset() > Integer.MAX_VALUE) { return new PageResult<>(List.of(), request.page(), request.size(), data.count()); }
        var page = data.findAll(org.springframework.data.domain.PageRequest.of(request.page(), request.size(), Sort.by("id")));
        return new PageResult<>(page.getContent().stream().map(CategoryEntity::toDomain).toList(), request.page(), request.size(), page.getTotalElements());
    }
    public Optional<Category> update(long id, Category value) {
        return data.findForUpdate(id).map(entity -> { entity.rename(value.getName()); return data.saveAndFlush(entity).toDomain(); });
    }
    public boolean deleteById(long id) {
        var entity = data.findForUpdate(id);
        if (entity.isEmpty()) { return false; }
        data.delete(entity.get()); data.flush(); return true;
    }
    public boolean existsById(long id) { return data.existsById(id); }
}
