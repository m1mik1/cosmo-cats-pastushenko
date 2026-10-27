package ua.kpi.pastushenko.cosmocats.api.dto;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category value) { return new CategoryResponse(value.getId(), value.getName()); }
}
