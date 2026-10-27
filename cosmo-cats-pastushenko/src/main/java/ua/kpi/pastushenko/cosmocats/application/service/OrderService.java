package ua.kpi.pastushenko.cosmocats.application.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.domain.model.*;
import ua.kpi.pastushenko.cosmocats.domain.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    public OrderService(OrderRepository orders, ProductRepository products) { this.orders = orders; this.products = products; }
    public PageResult<Order> findPage(int page, int size) { return orders.findPage(new PageRequest(page, size)); }
    public Order getById(long id) {
        requireId(id); return orders.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
    @Transactional
    public Order create(List<CartItem> items) {
        // PostgreSQL timestamptz stores microseconds; return the same precision that will be read back.
        return orders.create(new Order(null, snapshots(items), Instant.now().truncatedTo(ChronoUnit.MICROS)));
    }
    @Transactional
    public Order update(long id, List<CartItem> items) {
        var original = getById(id);
        var replacement = new Order(id, snapshots(items), original.getCreatedAt());
        return orders.update(id, replacement).orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }
    @Transactional
    public void delete(long id) { requireId(id); if (!orders.deleteById(id)) { throw new ResourceNotFoundException("Order", id); } }
    private List<OrderItem> snapshots(List<CartItem> items) {
        if (items == null || items.isEmpty() || items.size() > 100) { throw new IllegalArgumentException("order must contain between 1 and 100 items"); }
        var ids = new HashSet<Long>();
        return items.stream().map(item -> {
            if (item == null || !ids.add(item.getProductId())) { throw new IllegalArgumentException("order items must be non-null and product IDs must be unique"); }
            var product = products.findById(item.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product", item.getProductId()));
            return new OrderItem(product.getId(), product.getName(), product.getPrice(), item.getQuantity());
        }).toList();
    }
    private void requireId(long id) { if (id <= 0) { throw new IllegalArgumentException("orderId must be positive"); } }
}
