package ua.kpi.pastushenko.cosmocats.infrastructure.repository;

import org.springframework.stereotype.Repository;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;

import java.util.Map;

@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<Long, Category> categories = Map.of(
            1L, new Category(1L, "Space Food"),
            2L, new Category(2L, "Cosmic Toys"),
            3L, new Category(3L, "Space Accessories")
    );

    @Override
    public boolean existsById(long id) {
        return categories.containsKey(id);
    }
}
