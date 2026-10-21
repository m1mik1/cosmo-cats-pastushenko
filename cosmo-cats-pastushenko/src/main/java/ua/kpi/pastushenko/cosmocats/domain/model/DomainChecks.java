package ua.kpi.pastushenko.cosmocats.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class DomainChecks {

    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    private DomainChecks() {
    }

    static void optionalId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
    }

    static void positiveId(long id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }

    static void name(String name, int minimumLength) {
        if (name == null || name.isBlank()
                || name.length() < minimumLength || name.length() > 120) {
            throw new IllegalArgumentException(
                    "name must contain between " + minimumLength + " and 120 characters and must not be blank"
            );
        }
    }

    static BigDecimal price(BigDecimal price) {
        if (price == null || price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("price must be between 0.01 and 9999999999.99");
        }
        try {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("price must be a multiple of 0.01", exception);
        }
    }

    static void quantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
