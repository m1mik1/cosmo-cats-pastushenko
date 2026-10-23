package ua.kpi.pastushenko.cosmocats.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DomainRulesTest {
    @ParameterizedTest
    @ValueSource(strings = {"0.01", "12.500", "9999999999.99"})
    void pricesAreExactAndNormalizedToCents(String value) {
        var price = new BigDecimal(value);
        assertThat(product("Cosmic Milk", price).getPrice()).isEqualByComparingTo(price).hasScaleOf(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "10000000000.00", "12.345"})
    void invalidPriceIsRejectedInsteadOfRounded(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> product("Cosmic Milk", new BigDecimal(value)));
    }

    @Test
    void priceMustBePresent() {
        assertThatIllegalArgumentException().isThrownBy(() -> product("Cosmic Milk", null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "ab"})
    void nameCannotBeMissingBlankOrTooShort(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> product(name, BigDecimal.ONE));
    }

    @Test
    void nameAndDescriptionHaveInclusiveLengthLimits() {
        var valid = new Product(null, "x".repeat(120), "x".repeat(2000), BigDecimal.ONE, 1, 0);
        assertThat(valid.getName()).hasSize(120);
        assertThat(valid.getDescription()).hasSize(2000);
        assertThat(valid.getStockQuantity()).isZero();
        assertThatIllegalArgumentException().isThrownBy(() -> product("x".repeat(121), BigDecimal.ONE));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new Product(null, "Milk", "x".repeat(2001), BigDecimal.ONE, 1, 0));
    }

    @Test
    void invalidIdentifiersAndStockAreRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(0L, "Milk", null, BigDecimal.ONE, 1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(null, "Milk", null, BigDecimal.ONE, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new Product(null, "Milk", null, BigDecimal.ONE, 1, -1));
    }

    @Test
    void assigningAnIdDoesNotMutateTheOriginalProduct() {
        var original = product("Cosmic Milk", new BigDecimal("12.50"));
        var stored = original.withId(7);
        assertThat(original.getId()).isNull();
        assertThat(stored).usingRecursiveComparison().ignoringFields("id").isEqualTo(original);
        assertThat(stored.getId()).isEqualTo(7);
        assertThatIllegalArgumentException().isThrownBy(() -> original.withId(-1));
    }

    @Test
    void categoryAcceptsOneCharacterButNotBlankText() {
        var category = new Category(1L, "A");
        assertThat(category.getId()).isEqualTo(1);
        assertThat(category.getName()).isEqualTo("A");
        assertThatIllegalArgumentException().isThrownBy(() -> new Category(null, " "));
    }

    @Test
    void cartTakesAnImmutableSnapshotOfItems() {
        var item = new CartItem(7, 2);
        var source = new ArrayList<>(List.of(item));
        var cart = new Cart(1L, source);
        source.clear();
        assertThat(cart.getId()).isEqualTo(1);
        assertThat(cart.getItems()).containsExactly(item);
        assertThat(item.getProductId()).isEqualTo(7);
        assertThat(item.getQuantity()).isEqualTo(2);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> cart.getItems().clear());
        assertThat(new Cart(null, List.of()).getItems()).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> new Cart(null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new CartItem(7, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new CartItem(0, 1));
    }

    @Test
    void orderPreservesPriceSnapshotsAndAddsMoneyExactly() {
        var item = new OrderItem(7, "Milk", new BigDecimal("0.10"), 3);
        var source = new ArrayList<>(List.of(item, new OrderItem(8, "Yarn", new BigDecimal("0.20"), 1)));
        var createdAt = Instant.parse("2026-10-21T12:00:00Z");
        var order = new Order(2L, source, createdAt);
        source.clear();
        assertThat(order.getId()).isEqualTo(2);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("0.50");
        assertThat(item.getProductId()).isEqualTo(7);
        assertThat(item.getProductName()).isEqualTo("Milk");
        assertThat(item.getUnitPrice()).isEqualByComparingTo("0.10");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getLineTotal()).isEqualByComparingTo("0.30");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> order.getItems().clear());
    }

    @Test
    void emptyOrdersAndMissingTimestampAreRejected() {
        var item = new OrderItem(7, "Milk", BigDecimal.ONE, 1);
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(null, null, Instant.EPOCH));
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(null, List.of(), Instant.EPOCH));
        assertThatIllegalArgumentException().isThrownBy(() -> new Order(null, List.of(item), null));
        assertThatIllegalArgumentException().isThrownBy(() -> new OrderItem(7, "Milk", BigDecimal.ONE, -1));
    }

    @Test
    void paginationUsesLongOffsetAndRoundsPageCountUp() {
        assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset()).isEqualTo(214748364700L);
        assertThat(new ProductPage(List.of(), 0, 2, 0).totalPages()).isZero();
        assertThat(new ProductPage(List.of(), 0, 2, 4).totalPages()).isEqualTo(2);
        assertThat(new ProductPage(List.of(), 0, 2, 5).totalPages()).isEqualTo(3);
        var source = new ArrayList<>(List.of(product("Milk", BigDecimal.ONE)));
        var page = new ProductPage(source, 0, 1, 1);
        source.clear();
        assertThat(page.content()).hasSize(1);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> page.content().clear());
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductPage(List.of(), 0, 1, -1));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductPage(page.content(), 0, 1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProductPage(List.of(page.content().getFirst(), page.content().getFirst()), 0, 1, 2));
    }

    private static Product product(String name, BigDecimal price) {
        return new Product(null, name, null, price, 1, 10);
    }
}
