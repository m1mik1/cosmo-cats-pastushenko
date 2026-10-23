package ua.kpi.pastushenko.cosmocats.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.domain.model.PageRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.domain.model.ProductPage;
import ua.kpi.pastushenko.cosmocats.domain.repository.CategoryRepository;
import ua.kpi.pastushenko.cosmocats.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories);
    }

    @Test
    void findPagePreservesPaginationAndRepositoryResult() {
        var page = new ProductPage(List.of(product(null, 1).withId(7)), 2, 3, 7);
        when(products.findPage(new PageRequest(2, 3))).thenReturn(page);
        assertThat(service.findPage(2, 3)).isSameAs(page);
        verifyNoInteractions(categories);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,101"})
    void invalidPaginationDoesNotReachRepository(int page, int size) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.findPage(page, size));
        verifyNoInteractions(products, categories);
    }

    @Test
    void getByIdReturnsExistingProduct() {
        var stored = product(7L, 1);
        when(products.findById(7)).thenReturn(Optional.of(stored));
        assertThat(service.getById(7)).isSameAs(stored);
        verifyNoInteractions(categories);
    }

    @Test
    void missingProductIsNotFound() {
        when(products.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 99 was not found");
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void invalidIdsAreRejectedBeforeRepositoryCalls(long id) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.getById(id));
        assertThatIllegalArgumentException().isThrownBy(() -> service.update(id, product(null, 1)));
        assertThatIllegalArgumentException().isThrownBy(() -> service.delete(id));
        verifyNoInteractions(products, categories);
    }

    @Test
    void createChecksCategoryAndReturnsServerAssignedId() {
        var input = product(null, 2);
        var stored = input.withId(7);
        when(categories.existsById(2)).thenReturn(true);
        when(products.create(input)).thenReturn(stored);
        assertThat(service.create(input)).isSameAs(stored);
        assertThat(input.getId()).isNull();
        var order = inOrder(categories, products);
        order.verify(categories).existsById(2);
        order.verify(products).create(input);
    }

    @Test
    void createRejectsClientAssignedId() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.create(product(7L, 1)))
                .withMessageContaining("must not have an id");
        verifyNoInteractions(products, categories);
    }

    @Test
    void createWithMissingCategoryDoesNotWriteProduct() {
        when(categories.existsById(99)).thenReturn(false);
        assertThatThrownBy(() -> service.create(product(null, 99)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with id 99 was not found");
        verifyNoInteractions(products);
    }

    @Test
    void nullProductIsRejectedOnBothWrites() {
        assertThatNullPointerException().isThrownBy(() -> service.create(null));
        assertThatNullPointerException().isThrownBy(() -> service.update(7, null));
        verifyNoInteractions(products, categories);
    }

    @Test
    void updateAcceptsPayloadWithoutId() {
        var input = product(null, 2);
        var stored = input.withId(7);
        when(categories.existsById(2)).thenReturn(true);
        when(products.update(7, input)).thenReturn(Optional.of(stored));
        assertThat(service.update(7, input)).isSameAs(stored);
        verify(products, never()).create(any());
    }

    @Test
    void updateAcceptsMatchingDomainId() {
        var input = product(7L, 1);
        when(categories.existsById(1)).thenReturn(true);
        when(products.update(7, input)).thenReturn(Optional.of(input));
        assertThat(service.update(7, input)).isSameAs(input);
    }

    @Test
    void updateRejectsMismatchedId() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.update(7, product(8L, 1)))
                .withMessageContaining("must match");
        verifyNoInteractions(products, categories);
    }

    @Test
    void updateWithMissingCategoryDoesNotWriteProduct() {
        when(categories.existsById(99)).thenReturn(false);
        assertThatThrownBy(() -> service.update(7, product(null, 99)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with id 99 was not found");
        verifyNoInteractions(products);
    }

    @Test
    void updateMissingProductDoesNotCreateIt() {
        var input = product(null, 1);
        when(categories.existsById(1)).thenReturn(true);
        when(products.update(99, input)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(99, input))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 99 was not found");
        verify(products, never()).create(any());
    }

    @Test
    void deleteExistingProductCompletes() {
        when(products.deleteById(7)).thenReturn(true);
        assertThatCode(() -> service.delete(7)).doesNotThrowAnyException();
        verify(products).deleteById(7);
        verifyNoInteractions(categories);
    }

    @Test
    void deleteMissingProductIsNotFound() {
        when(products.deleteById(99)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 99 was not found");
    }

    private static Product product(Long id, long categoryId) {
        return new Product(id, "Stellar Treats", "Space food", new BigDecimal("12.50"), categoryId, 10);
    }
}
