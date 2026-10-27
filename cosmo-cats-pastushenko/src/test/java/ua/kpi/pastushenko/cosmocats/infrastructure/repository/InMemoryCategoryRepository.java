package ua.kpi.pastushenko.cosmocats.infrastructure.repository;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;
import java.util.*;
public class InMemoryCategoryRepository implements CategoryRepository {
    private final Map<Long,Category> data = new TreeMap<>(Map.of(1L,new Category(1L,"Space Food"),2L,new Category(2L,"Cosmic Toys"),3L,new Category(3L,"Space Accessories")));
    private long nextId = 4;
    public boolean existsById(long id) { return data.containsKey(id); }
    public Category create(Category value) { var saved = new Category(nextId++,value.getName()); data.put(saved.getId(),saved); return saved; }
    public Optional<Category> findById(long id) { return Optional.ofNullable(data.get(id)); }
    public PageResult<Category> findPage(PageRequest request) { return new PageResult<>(data.values().stream().skip(request.offset()).limit(request.size()).toList(), request.page(),request.size(),data.size()); }
    public Optional<Category> update(long id, Category value) {
        if (!existsById(id)) { return Optional.empty(); }
        var saved = new Category(id,value.getName()); data.put(id,saved); return Optional.of(saved);
    }
    public boolean deleteById(long id) { return data.remove(id)!=null; }
}
