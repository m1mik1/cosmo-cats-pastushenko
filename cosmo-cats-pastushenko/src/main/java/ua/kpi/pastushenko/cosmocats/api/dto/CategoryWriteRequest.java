package ua.kpi.pastushenko.cosmocats.api.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CategoryWriteRequest(@NotBlank @Size(max = 120) String name) {}
