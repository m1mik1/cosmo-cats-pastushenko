package ua.kpi.pastushenko.cosmocats.api.dto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import ua.kpi.pastushenko.cosmocats.domain.model.Order;
public record OrderResponse(Long id, Instant createdAt, List<Item> items, BigDecimal totalAmount) {
    public record Item(long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public static OrderResponse from(Order value) {
        return new OrderResponse(value.getId(), value.getCreatedAt(), value.getItems().stream()
                .map(item -> new Item(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal())).toList(), value.getTotalAmount());
    }
}
