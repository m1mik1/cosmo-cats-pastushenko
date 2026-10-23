package ua.kpi.pastushenko.cosmocats.infrastructure.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ua.kpi.pastushenko.cosmocats.domain.model.PageRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class InMemoryProductRepositoryTest {
    InMemoryProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProductRepository();
    }

    @Test
    void pagesAreSortedStableAndHaveCorrectTotals() {
        var first = repository.findPage(new PageRequest(0, 2));
        var second = repository.findPage(new PageRequest(1, 2));
        assertThat(first.content()).extracting(Product::getId).containsExactly(1L, 2L);
        assertThat(second.content()).extracting(Product::getId).containsExactly(3L);
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(repository.findPage(new PageRequest(Integer.MAX_VALUE, 100)).content()).isEmpty();
        assertThat(repository.findPage(new PageRequest(0, 2))).isEqualTo(first);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> first.content().clear());
    }

    @Test
    void createdIdsAreUniqueAndNotReusedAfterDeletion() {
        var input = product();
        var first = repository.create(input);
        assertThat(first.getId()).isEqualTo(4);
        assertThat(input.getId()).isNull();
        assertThat(repository.findById(4)).contains(first);
        assertThat(repository.deleteById(4)).isTrue();
        assertThat(repository.create(input).getId()).isEqualTo(5);
        assertThat(repository.findById(4)).isEmpty();
    }

    @Test
    void updateReplacesAllFieldsWithoutCreatingANewProduct() {
        var original = repository.findById(1).orElseThrow();
        var replacement = product();
        var updated = repository.update(1, replacement).orElseThrow();
        assertThat(updated.getId()).isEqualTo(1);
        assertThat(updated).usingRecursiveComparison().ignoringFields("id").isEqualTo(replacement);
        assertThat(original.getName()).isEqualTo("Cosmic Milk");
        assertThat(repository.findById(1)).contains(updated);
        assertThat(repository.findPage(new PageRequest(0, 20)).totalElements()).isEqualTo(3);
        assertThat(repository.update(99, replacement)).isEmpty();
        assertThat(repository.findById(99)).isEmpty();
    }

    @Test
    void deletingTwiceReportsMissingProduct() {
        assertThat(repository.deleteById(1)).isTrue();
        assertThat(repository.deleteById(1)).isFalse();
        assertThat(repository.findById(1)).isEmpty();
        assertThat(repository.findPage(new PageRequest(0, 20)).totalElements()).isEqualTo(2);
    }

    @Test
    void createCannotOverwriteExistingIdAndNullArgumentsAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> repository.create(product().withId(1)));
        assertThat(repository.findById(1).orElseThrow().getName()).isEqualTo("Cosmic Milk");
        assertThatNullPointerException().isThrownBy(() -> repository.create(null));
        assertThatNullPointerException().isThrownBy(() -> repository.update(1, null));
        assertThatNullPointerException().isThrownBy(() -> repository.findPage(null));
    }

    @Test
    void onlySeededCategoriesExist() {
        var categories = new InMemoryCategoryRepository();
        assertThat(categories.existsById(1)).isTrue();
        assertThat(categories.existsById(2)).isTrue();
        assertThat(categories.existsById(3)).isTrue();
        assertThat(categories.existsById(0)).isFalse();
        assertThat(categories.existsById(99)).isFalse();
    }

    private static Product product() {
        return new Product(null, "New Collar", null, new BigDecimal("19.99"), 3, 0);
    }
}
