package ua.kpi.pastushenko.cosmocats.api.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        long totalPages
) {
    public ProductPageResponse {
        content = List.copyOf(content);
    }
}
