package ua.kpi.pastushenko.cosmocats.infrastructure.persistence;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.OrderRepository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.entity.OrderEntity;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public class JpaOrderRepository implements OrderRepository {
    private final SpringDataOrderRepository data;
    private final SpringDataProductRepository references;
    public JpaOrderRepository(SpringDataOrderRepository data, SpringDataProductRepository references) { this.data = data; this.references = references; }
    public Order create(Order value) { return data.saveAndFlush(new OrderEntity(value, references::getReferenceById)).toDomain(); }
    public Optional<Order> findById(long id) { return data.findById(id).map(OrderEntity::toDomain); }
    public PageResult<Order> findPage(PageRequest request) {
        // JPA uses an int offset. Preserve the API's long arithmetic for large pages.
        if (request.offset() > Integer.MAX_VALUE) { return new PageResult<>(List.of(), request.page(), request.size(), data.count()); }
        var page = data.findAll(org.springframework.data.domain.PageRequest.of(request.page(), request.size(), Sort.by("id")));
        return new PageResult<>(page.getContent().stream().map(OrderEntity::toDomain).toList(), request.page(), request.size(), page.getTotalElements());
    }
    public Optional<Order> update(long id, Order value) {
        return data.findForUpdate(id).map(entity -> { entity.replaceItems(value, references::getReferenceById); return data.saveAndFlush(entity).toDomain(); });
    }
    public boolean deleteById(long id) {
        var entity = data.findForUpdate(id);
        if (entity.isEmpty()) { return false; }
        data.delete(entity.get()); data.flush(); return true;
    }

}
