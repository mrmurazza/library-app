package com.lexhive.libraryapp.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lexhive.libraryapp.config.LibraryConfig;
import com.lexhive.libraryapp.dto.LoanResponse;
import com.lexhive.libraryapp.dto.LoanStatus;
import com.lexhive.libraryapp.entity.Book;
import com.lexhive.libraryapp.entity.Loan;
import com.lexhive.libraryapp.entity.Member;
import com.lexhive.libraryapp.exception.CustomException;
import com.lexhive.libraryapp.exception.CustomException.Code;
import com.lexhive.libraryapp.repository.BookRepository;
import com.lexhive.libraryapp.repository.LoanRepository;
import com.lexhive.libraryapp.repository.MemberRepository;
import com.lexhive.libraryapp.security.CurrentUser;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final LibraryConfig libraryConfig;

    public LoanService(
            LoanRepository loanRepository,
            BookRepository bookRepository,
            MemberRepository memberRepository,
            LibraryConfig libraryConfig
    ) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.libraryConfig = libraryConfig;
    }

    @Transactional
    public LoanResponse createLoan(long bookId, CurrentUser user) {
        long memberId = getValidMemberId(user);
        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> CustomException.notFound(Code.MEMBER_NOT_FOUND, String.format("Member %d was not found", memberId)));

        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> CustomException.notFound(Code.BOOK_NOT_FOUND, String.format("Book %d was not found", bookId)));

        List<Loan> ongoingLoans = loanRepository.findByMember_IdAndReturnedAtIsNull(memberId);
        Instant borrowedAt = Instant.now();

        validateBorrowing(book,borrowedAt, ongoingLoans);

        Instant dueDate = borrowedAt.plus(libraryConfig.loanDurationDays(), ChronoUnit.DAYS);
        book.setAvailableCopies(book.getAvailableCopies() - 1);

        Loan loan = loanRepository.save(new Loan(book, member, borrowedAt, dueDate));

        return LoanResponse.fromEntity(loan);
    }

    private void validateBorrowing(Book book, Instant borrowedAt, List<Loan> loans) throws CustomException {
        if (book.getAvailableCopies() <= 0) {
            throw CustomException.unprocessableEntity(Code.BOOK_UNAVAILABLE, String.format("No copies of book %d are available", book.getId()));
        }

        List<Instant> dueDates = loans.stream().map(Loan::getDueDate).toList();
        if (dueDates.stream().anyMatch(dueDate -> dueDate.isBefore(borrowedAt))) {
            throw CustomException.unprocessableEntity(Code.MEMBER_HAS_OVERDUE_LOAN, "Member has an overdue loan and cannot borrow");
        }
        if (dueDates.size() >= libraryConfig.maxActiveLoans()) {
            throw CustomException.unprocessableEntity(
                    Code.LOAN_LIMIT_REACHED,
                    String.format("Member already has %d active loans", dueDates.size()));
        }
    }

    @Transactional
    public LoanResponse returnLoan(long loanId, CurrentUser user) {
        Loan loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> CustomException.notFound(Code.LOAN_NOT_FOUND, String.format("Loan %d was not found", loanId)));

        validateReturn(loan, user);

        Book book = bookRepository.findByIdForUpdate(loan.getBook().getId()).orElseThrow(() ->
                CustomException.notFound(Code.BOOK_NOT_FOUND, String.format("Book %d was not found", loan.getBook().getId())));

        loan.setReturnedAt(Instant.now());
        book.setAvailableCopies(book.getAvailableCopies() + 1);

        return LoanResponse.fromEntity(loan);
    }

    private void validateReturn(Loan loan, CurrentUser user) throws CustomException {
        if (!loan.getMember().getId().equals(getValidMemberId(user))) {
            throw CustomException.forbidden(Code.FORBIDDEN, String.format("Member %d can only return their own loans", loan.getMember().getId()));
        }

        if (loan.getReturnedAt() != null) {
            throw CustomException.unprocessableEntity(Code.LOAN_ALREADY_RETURNED, String.format("Loan %d is already returned", loan.getId()));
        }
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> list(LoanStatus status, CurrentUser user) {
        Long memberId = user.isAdmin() ? null : user.memberId();
        return loanRepository.findByMemberIdAndStatus(memberId, status).stream()
                .map(LoanResponse::fromEntity)
                .toList();
    }

    private long getValidMemberId(CurrentUser user) {
        if (user.memberId() == null) {
            throw CustomException.forbidden(Code.FORBIDDEN, "Only members can borrow or return books");
        }

        return user.memberId();
    }
}
