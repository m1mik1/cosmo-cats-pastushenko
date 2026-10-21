package ua.kpi.pastushenko.cosmocats.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public final class CentAmountValidator implements ConstraintValidator<CentAmount, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // Null is checked by @NotNull; trailing zeros do not change the amount.
        return value == null || value.stripTrailingZeros().scale() <= 2;
    }
}
