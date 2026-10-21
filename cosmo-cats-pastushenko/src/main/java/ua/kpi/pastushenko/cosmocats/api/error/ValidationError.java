package ua.kpi.pastushenko.cosmocats.api.error;

public record ValidationError(String field, String message) {
}
