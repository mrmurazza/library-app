package com.lexhive.libraryapp.dto;

import jakarta.validation.constraints.NotNull;

public record CreateLoanRequest(
        @NotNull Long bookId
) {
}
