package ua.kpi.pastushenko.cosmocats.api.dto;

import java.math.BigDecimal;

public record ProductResponse(
        long id,
        String name,
        String description,
        BigDecimal price,
        long categoryId,
        int stockQuantity
) {
}
