package com.lexhive.libraryapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateBookRequest(
        @Size(min = 1, max = 255) String title,
        @Size(min = 1, max = 255) String author,
        @Size(min = 1, max = 32) String isbn,
        @Min(1) Integer totalCopies
) {
}
