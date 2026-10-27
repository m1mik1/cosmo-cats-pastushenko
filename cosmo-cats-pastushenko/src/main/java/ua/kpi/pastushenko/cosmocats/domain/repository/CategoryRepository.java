package ua.kpi.pastushenko.cosmocats.domain.repository;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import java.util.Optional;
public interface CategoryRepository {
    Category create(Category value);
    Optional<Category> findById(long id);
    PageResult<Category> findPage(PageRequest request);
    Optional<Category> update(long id, Category value);
    boolean deleteById(long id);
    boolean existsById(long id);
}
