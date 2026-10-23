package ua.kpi.pastushenko.cosmocats.api.mapper;

import org.junit.jupiter.api.Test;
import ua.kpi.pastushenko.cosmocats.api.dto.ProductWriteRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductMapperTest {
    private final ProductMapper mapper = new ProductMapper();

    @Test
    void roundTripPreservesAllFieldsAndAssignsIdOnlyAfterPersistence() {
        var request = new ProductWriteRequest("Stellar Treats", "Space food", new BigDecimal("12.500"), 2L, 10);
        var domain = mapper.toDomain(request);
        assertThat(domain.getId()).isNull();
        var response = mapper.toDto(domain.withId(7));
        assertThat(response.id()).isEqualTo(7);
        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.description()).isEqualTo(request.description());
        assertThat(response.price()).isEqualByComparingTo(request.price()).hasScaleOf(2);
        assertThat(response.categoryId()).isEqualTo(request.categoryId());
        assertThat(response.stockQuantity()).isEqualTo(request.stockQuantity());
    }

    @Test
    void missingRequiredNumbersCannotBecomePrimitiveDefaults() {
        assertThatIllegalArgumentException().isThrownBy(() -> mapper.toDomain(
                new ProductWriteRequest("Milk", null, BigDecimal.ONE, null, 0)));
        assertThatIllegalArgumentException().isThrownBy(() -> mapper.toDomain(
                new ProductWriteRequest("Milk", null, BigDecimal.ONE, 1L, null)));
    }

    @Test
    void unsavedProductCannotBeExposedAsAnApiResponse() {
        var product = new Product(null, "Milk", null, BigDecimal.ONE, 1, 0);
        assertThatIllegalArgumentException().isThrownBy(() -> mapper.toDto(product));
    }

    @Test
    void nullInputIsRejectedExplicitly() {
        assertThatNullPointerException().isThrownBy(() -> mapper.toDomain(null));
        assertThatNullPointerException().isThrownBy(() -> mapper.toDto(null));
    }
}
