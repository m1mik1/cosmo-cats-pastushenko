package ua.kpi.pastushenko.cosmocats.api.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import ua.kpi.pastushenko.cosmocats.domain.model.CartItem;
public record OrderWriteRequest(@NotNull @Size(min = 1, max = 100) List<@NotNull @Valid Item> items) {
    public record Item(@NotNull @Positive Long productId, @NotNull @Positive Integer quantity) {}
    public List<CartItem> toItems() { return items.stream().map(item -> new CartItem(item.productId(), item.quantity())).toList(); }
}
