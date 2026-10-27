package ua.kpi.pastushenko.cosmocats.api.dto;
import java.util.List;
import java.util.function.Function;
import ua.kpi.pastushenko.cosmocats.domain.model.PageResult;
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, long totalPages) {
    public static <D,T> PageResponse<T> from(PageResult<D> value, Function<D,T> mapper) {
        return new PageResponse<>(value.content().stream().map(mapper).toList(), value.page(), value.size(), value.totalElements(), value.totalPages());
    }
}
