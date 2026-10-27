package ua.kpi.pastushenko.cosmocats.application.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;
import java.util.Objects;
@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }
    public PageResult<Category> findPage(int page, int size) { return categories.findPage(new PageRequest(page, size)); }
    public Category getById(long id) {
        requireId(id); return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
    @Transactional
    public Category create(Category value) {
        Objects.requireNonNull(value, "category must not be null");
        if (value.getId() != null) { throw new IllegalArgumentException("new category must not have an id"); }
        return categories.create(value);
    }
    @Transactional
    public Category update(long id, Category value) {
        requireId(id); Objects.requireNonNull(value, "category must not be null");
        if (value.getId() != null && value.getId() != id) { throw new IllegalArgumentException("category id must match the requested id"); }
        return categories.update(id, value).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
    @Transactional
    public void delete(long id) {
        requireId(id); if (!categories.deleteById(id)) { throw new ResourceNotFoundException("Category", id); }
    }
    private void requireId(long id) { if (id <= 0) { throw new IllegalArgumentException("categoryId must be positive"); } }
}
