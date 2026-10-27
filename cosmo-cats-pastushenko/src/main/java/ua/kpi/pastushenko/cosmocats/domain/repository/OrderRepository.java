package ua.kpi.pastushenko.cosmocats.domain.repository;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import java.util.Optional;
public interface OrderRepository {
    Order create(Order value);
    Optional<Order> findById(long id);
    PageResult<Order> findPage(PageRequest request);
    Optional<Order> update(long id, Order value);
    boolean deleteById(long id);

}
