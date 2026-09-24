package com.lexhive.libraryapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "library")
public record LibraryConfig(
        @Min(1) int maxActiveLoans,
        @Min(1) int loanDurationDays
) {
}
