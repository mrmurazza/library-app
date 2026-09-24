package com.lexhive.libraryapp.dto;

import java.time.Instant;

import com.lexhive.libraryapp.entity.Loan;

public record LoanResponse(
        Long id,
        Long bookId,
        Long memberId,
        Instant borrowedAt,
        Instant dueDate,
        Instant returnedAt
) {

    public static LoanResponse fromEntity(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getMember().getId(),
                loan.getBorrowedAt(),
                loan.getDueDate(),
                loan.getReturnedAt());
    }
}
