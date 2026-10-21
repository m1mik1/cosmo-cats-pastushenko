package ua.kpi.pastushenko.cosmocats.domain.model;

import java.util.List;

public record ProductPage(List<Product> content, int page, int size, long totalElements) {

    public ProductPage {
        new PageRequest(page, size);
        content = List.copyOf(content);
        if (totalElements < 0 || content.size() > size || content.size() > totalElements) {
            throw new IllegalArgumentException("invalid product page metadata");
        }
    }

    public long totalPages() {
        return totalElements / size + (totalElements % size == 0 ? 0 : 1);
    }
}
