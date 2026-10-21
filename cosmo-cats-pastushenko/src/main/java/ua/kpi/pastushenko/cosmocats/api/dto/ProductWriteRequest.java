package ua.kpi.pastushenko.cosmocats.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductWriteRequest(
        @NotBlank @Size(min = 3, max = 120) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") BigDecimal price,
        @NotNull @Positive Long categoryId,
        @NotNull @PositiveOrZero Integer stockQuantity
) {
}
