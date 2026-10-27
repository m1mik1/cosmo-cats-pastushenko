package ua.kpi.pastushenko.cosmocats.integration;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import ua.kpi.pastushenko.cosmocats.application.exception.ResourceNotFoundException;
import ua.kpi.pastushenko.cosmocats.application.service.CategoryService;
import ua.kpi.pastushenko.cosmocats.application.service.OrderService;
import ua.kpi.pastushenko.cosmocats.application.service.ProductService;
import ua.kpi.pastushenko.cosmocats.domain.model.CartItem;
import ua.kpi.pastushenko.cosmocats.domain.model.Category;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.JpaCategoryRepository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.JpaOrderRepository;
import ua.kpi.pastushenko.cosmocats.infrastructure.persistence.JpaProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestConfiguration.class, JpaCategoryRepository.class,
        JpaProductRepository.class, JpaOrderRepository.class,
        CategoryService.class, ProductService.class, OrderService.class})
// Test-managed rollback could hide a missing @Transactional on a service.
// Each service call must commit or roll back its own transaction instead.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DatabaseCrudIT {
    @Autowired CategoryService categories;
    @Autowired ProductService products;
    @Autowired OrderService orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpringLiquibase liquibase;
    @Autowired ApplicationContext context;

    @BeforeEach
    void resetContainerData() {
        // @ServiceConnection supplies a dedicated Testcontainers database.
        // No connection to the development Compose database is used here.
        jdbc.execute("ALTER TABLE order_items DROP CONSTRAINT IF EXISTS test_reject_quantity");
        jdbc.execute("TRUNCATE TABLE order_items, orders, products, categories RESTART IDENTITY CASCADE");
    }

    @Test
    void usesMigratedPostgresAndOnlyThePersistenceSlice() {
        assertThat(jdbc.queryForObject("SELECT version()", String.class)).startsWith("PostgreSQL 17.");
        assertThat(jdbc.queryForList("SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class))
                .containsExactly("001-create-schema", "002-add-constraints-and-indexes", "003-seed-catalog");
        assertThat(jdbc.queryForList("SELECT sequencename FROM pg_sequences WHERE schemaname='public' AND increment_by=1", String.class))
                .contains("categories_id_seq", "products_id_seq", "orders_id_seq", "order_items_id_seq");
        assertThat(context.getBeansWithAnnotation(RestController.class)).isEmpty();
    }

    @Test
    void categoryCrudCommitsBetweenServiceCalls() {
        var category = category("Space Food");
        assertThat(category.getId()).isPositive();
        assertThat(categories.getById(category.getId()).getName()).isEqualTo("Space Food");
        assertThat(categories.findPage(0, 1).content()).extracting(Category::getId).containsExactly(category.getId());
        var updated = categories.update(category.getId(), new Category(null, "Renamed Food"));
        assertThat(updated.getId()).isEqualTo(category.getId());
        assertThat(categories.getById(category.getId()).getName()).isEqualTo("Renamed Food");
        categories.delete(category.getId());
        assertThat(count("categories")).isZero();
        assertThatThrownBy(() -> categories.getById(category.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void productCrudPreservesIdAndSupportsChangingCategoryAndClearingDescription() {
        var first = category("Food");
        var second = category("Toys");
        var product = products.create(new Product(null, "Space Milk", "Original description", new BigDecimal("12.50"), first.getId(), 10));
        assertThat(products.getById(product.getId()).getPrice()).isEqualByComparingTo("12.50");
        var updated = products.update(product.getId(), new Product(null, "Space Yarn", null, new BigDecimal("7.99"), second.getId(), 0));
        var stored = products.getById(product.getId());
        assertThat(updated.getId()).isEqualTo(product.getId());
        assertThat(stored.getDescription()).isNull();
        assertThat(stored.getName()).isEqualTo("Space Yarn");
        assertThat(stored.getCategoryId()).isEqualTo(second.getId());
        assertThat(stored.getStockQuantity()).isZero();
        products.delete(product.getId());
        assertThat(count("products")).isZero();
        assertThatThrownBy(() -> products.getById(product.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void orderCrudReplacesItemsAndDeletesOnlyItsOwnRows() {
        var category = category("Food");
        var first = product(category, "Space Milk", "12.50");
        var second = product(category, "Space Treats", "3.00");
        var created = orders.create(List.of(new CartItem(first.getId(), 2), new CartItem(second.getId(), 3)));
        assertThat(created.getTotalAmount()).isEqualByComparingTo("34.00");
        assertThat(orders.getById(created.getId()).getItems()).hasSize(2);
        assertThat(orders.findPage(0, 1).totalElements()).isEqualTo(1);
        var updated = orders.update(created.getId(), List.of(new CartItem(second.getId(), 4)));
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getTotalAmount()).isEqualByComparingTo("12.00");
        assertThat(orders.getById(created.getId()).getItems()).hasSize(1);
        assertThat(count("order_items")).isEqualTo(1);
        orders.delete(created.getId());
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
        assertThat(count("products")).isEqualTo(2);
        assertThatThrownBy(() -> orders.getById(created.getId())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void categoryNameIsUniqueInTheDatabase() {
        var original = category("Space Food");
        assertThatThrownBy(() -> category("Space Food")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("categories")).isEqualTo(1);
        assertThat(categories.getById(original.getId()).getName()).isEqualTo("Space Food");
    }

    @Test
    void productNamesAreUniqueWithinOneCategoryButAllowedInAnother() {
        var first = category("Food");
        var second = category("Toys");
        product(first, "Space Item", "1.00");
        assertThatThrownBy(() -> product(first, "Space Item", "2.00")).isInstanceOf(DataIntegrityViolationException.class);
        product(second, "Space Item", "2.00");
        assertThat(count("products")).isEqualTo(2);
    }

    @Test
    void conflictingProductUpdateRollsBackAllEditedFields() {
        var category = category("Food");
        product(category, "Space Milk", "12.50");
        var target = product(category, "Space Treats", "3.00");
        assertThatThrownBy(() -> products.update(target.getId(),
                new Product(null, "Space Milk", "Must not persist", new BigDecimal("99.00"), category.getId(), 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
        var stored = products.getById(target.getId());
        assertThat(stored.getName()).isEqualTo("Space Treats");
        assertThat(stored.getPrice()).isEqualByComparingTo("3.00");
        assertThat(stored.getDescription()).isNull();
        assertThat(stored.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void foreignKeysPreventDeletingReferencedCategoryAndProduct() {
        var category = category("Food");
        var product = product(category, "Space Milk", "12.50");
        var order = orders.create(List.of(new CartItem(product.getId(), 1)));
        assertThatThrownBy(() -> categories.delete(category.getId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> products.delete(product.getId())).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(categories.getById(category.getId()).getName()).isEqualTo("Food");
        assertThat(products.getById(product.getId()).getName()).isEqualTo("Space Milk");
        assertThat(orders.getById(order.getId()).getTotalAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void orderSnapshotsAndCreationTimeSurviveChangesToTheCatalog() {
        var category = category("Food");
        var product = product(category, "Original Milk", "12.50");
        var created = orders.create(List.of(new CartItem(product.getId(), 2)));
        products.update(product.getId(), new Product(null, "Changed Milk", null, new BigDecimal("99.00"), category.getId(), 10));
        var stored = orders.getById(created.getId());
        assertThat(stored.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(stored.getItems().getFirst().getProductName()).isEqualTo("Original Milk");
        assertThat(stored.getTotalAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void failedSecondOrderItemRollsBackTheHeaderAndAllItems() {
        var category = category("Food");
        var first = product(category, "Space Milk", "12.50");
        var second = product(category, "Space Treats", "3.00");
        rejectQuantityThirteen();
        try {
            assertThatThrownBy(() -> orders.create(List.of(new CartItem(first.getId(), 2), new CartItem(second.getId(), 13))))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(count("orders")).isZero();
            assertThat(count("order_items")).isZero();
            assertThat(count("products")).isEqualTo(2);
        } finally {
            removeFault();
        }
    }

    @Test
    void failedReplacementRestoresTheOriginalOrderItems() {
        var category = category("Food");
        var first = product(category, "Space Milk", "12.50");
        var second = product(category, "Space Treats", "3.00");
        var original = orders.create(List.of(new CartItem(first.getId(), 2), new CartItem(second.getId(), 3)));
        rejectQuantityThirteen();
        try {
            assertThatThrownBy(() -> orders.update(original.getId(), List.of(new CartItem(second.getId(), 13))))
                    .isInstanceOf(DataIntegrityViolationException.class);
            var stored = orders.getById(original.getId());
            assertThat(stored.getCreatedAt()).isEqualTo(original.getCreatedAt());
            assertThat(stored.getTotalAmount()).isEqualByComparingTo("34.00");
            assertThat(stored.getItems()).hasSize(2);
            assertThat(count("orders")).isEqualTo(1);
            assertThat(count("order_items")).isEqualTo(2);
        } finally {
            removeFault();
        }
    }

    @Test
    void missingReferencesAndMissingUpdateTargetsCannotCreatePartialData() {
        var category = category("Food");
        var product = product(category, "Space Milk", "12.50");
        assertThatThrownBy(() -> products.create(new Product(null, "Unknown Category", null, new BigDecimal("1.00"), 999999, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.create(List.of(new CartItem(product.getId(), 1), new CartItem(999999, 1))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> categories.update(999999, new Category(null, "Missing"))).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> products.update(999999, new Product(null, "Missing", null, new BigDecimal("1.00"), category.getId(), 1)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.update(999999, List.of(new CartItem(product.getId(), 1))))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(count("categories")).isEqualTo(1);
        assertThat(count("products")).isEqualTo(1);
        assertThat(count("orders")).isZero();
        assertThat(count("order_items")).isZero();
    }

    @Test
    void paginationIsStableAndHugeOffsetsReturnCorrectEmptyPages() {
        var category = category("Food");
        var first = product(category, "First Item", "1.00");
        var second = product(category, "Second Item", "2.00");
        assertThat(products.findPage(0, 1).content()).extracting(Product::getId).containsExactly(first.getId());
        assertThat(products.findPage(1, 1).content()).extracting(Product::getId).containsExactly(second.getId());
        assertThat(products.findPage(Integer.MAX_VALUE, 100).content()).isEmpty();
        assertThat(products.findPage(Integer.MAX_VALUE, 100).totalElements()).isEqualTo(2);
        assertThat(categories.findPage(Integer.MAX_VALUE, 100).content()).isEmpty();
        assertThat(categories.findPage(Integer.MAX_VALUE, 100).totalElements()).isEqualTo(1);
        orders.create(List.of(new CartItem(first.getId(), 1)));
        assertThat(orders.findPage(Integer.MAX_VALUE, 100).content()).isEmpty();
        assertThat(orders.findPage(Integer.MAX_VALUE, 100).totalElements()).isEqualTo(1);
    }

    @Test
    void sequenceDoesNotReuseDeletedIds() {
        var category = category("Food");
        var first = product(category, "First Item", "1.00");
        products.delete(first.getId());
        var next = product(category, "Next Item", "1.00");
        assertThat(next.getId()).isGreaterThan(first.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO products(name,price,category_id,stock_quantity) VALUES ('Invalid Price',0,1,1)",
            "INSERT INTO products(name,price,category_id,stock_quantity) VALUES ('Invalid Stock',1,1,-1)",
            "INSERT INTO products(name,price,category_id,stock_quantity) VALUES ('   ',1,1,1)"})
    void databaseRejectsInvalidRowsEvenWithoutTheJavaDomain(String sql) {
        category("Food");
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(count("products")).isZero();
    }

    @Test
    void liquibaseCanRunAgainWithoutRepeatingSeedData() throws Exception {
        var category = category("Food");
        var product = product(category, "Space Milk", "12.50");
        liquibase.afterPropertiesSet();
        assertThat(count("categories")).isEqualTo(1);
        assertThat(count("products")).isEqualTo(1);
        assertThat(products.getById(product.getId()).getName()).isEqualTo("Space Milk");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM databasechangelog", Long.class)).isEqualTo(3);
    }

    private Category category(String name) {
        return categories.create(new Category(null, name));
    }

    private Product product(Category category, String name, String price) {
        return products.create(new Product(null, name, null, new BigDecimal(price), category.getId(), 10));
    }

    private long count(String table) {
        // Only fixed table names from this test class are passed here.
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    private void rejectQuantityThirteen() {
        // A database failure after persistence begins proves the service's rollback.
        // This fault exists only in the temporary container, never in migrations.
        jdbc.execute("ALTER TABLE order_items ADD CONSTRAINT test_reject_quantity CHECK (quantity <> 13)");
    }

    private void removeFault() {
        jdbc.execute("ALTER TABLE order_items DROP CONSTRAINT test_reject_quantity");
    }
}
