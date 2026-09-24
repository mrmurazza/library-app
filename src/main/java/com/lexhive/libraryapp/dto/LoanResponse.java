package com.lexhive.libraryapp.dto;

import java.time.Instant;

import com.lexhive.libraryapp.entity.Book;
import com.lexhive.libraryapp.entity.Loan;

public record LoanResponse(
        Long id,
        Long bookId,
        BookResponse book,
        Long memberId,
        Instant borrowedAt,
        Instant dueDate,
        Instant returnedAt
) {

    public static LoanResponse fromEntity(Loan loan, Book book) {
        return new LoanResponse(
                loan.getId(),
                book.getId(),
                BookResponse.fromEntity(book),
                loan.getMember().getId(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt());
    }
}
