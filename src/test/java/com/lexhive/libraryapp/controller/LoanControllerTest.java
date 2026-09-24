package com.lexhive.libraryapp.controller;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class LoanControllerTest extends ControllerIntegrationTest {

    @Test
    @DisplayName("When anonymous request, must got unauthorized error")
    void anonymousRequestIsUnauthorized() throws Exception {
        var response = send(get("/api/loans"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(code(response)).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("When admin request tries to borrow a book, must got forbidden error")
    void librarianCannotBorrow() throws Exception {
        long bookId = populateBook("Dune", "isbn-dune", 1);

        var response = send(post("/api/loans")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(bookId)));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(code(response)).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("When a member borrows a book, the loan is theirs, the due date is set, and stock drops")
    void memberBorrowCreatesTheirLoanAndDecrementsStock() throws Exception {
        long razza = populateMember("Razza", "razza@razza.com");
        populateMember("Rafif", "rafif@rafif.com");
        long bookId = populateBook("Dune", "isbn-dune", 2);

        var created = send(post("/api/loans")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(bookId)));

        assertThat(created.getStatus()).isEqualTo(201);
        assertThat(read(created, "$.data.member_id", Number.class).longValue()).isEqualTo(razza);
        Instant borrowedAt = Instant.parse(read(created, "$.data.borrowed_at", String.class));
        Instant dueDate = Instant.parse(read(created, "$.data.due_date", String.class));
        assertThat(dueDate).isEqualTo(borrowedAt.plus(14, ChronoUnit.DAYS));
        assertThat(read(created, "$.data.returned_at", String.class)).isNull();

        var book = send(get("/api/books/{id}", bookId).with(user1()));
        assertThat(read(book, "$.data.available_copies", Integer.class)).isEqualTo(1);

        var razzaLoans = send(get("/api/loans").with(user1()));
        assertThat(razzaLoans.getStatus()).isEqualTo(200);
        assertThat(read(razzaLoans, "$.data.length()", Integer.class)).isEqualTo(1);

        var rafifLoans = send(get("/api/loans").with(user2()));
        assertThat(rafifLoans.getStatus()).isEqualTo(200);
        assertThat(read(rafifLoans, "$.data.length()", Integer.class)).isZero();
    }

    @Test
    @DisplayName("When member request tries to borrow a book, must be rejected when the loan limit is reached")
    void borrowIsRejectedAtTheActiveLoanLimit() throws Exception {
        populateMember("Razza", "razza@razza.com");
        long fourth = populateBook("Fourth", "isbn-4", 1);
        borrow(populateBook("One", "isbn-1", 1));
        borrow(populateBook("Two", "isbn-2", 1));
        borrow(populateBook("Three", "isbn-3", 1));

        var response = send(post("/api/loans")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(fourth)));

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(code(response)).isEqualTo("LOAN_LIMIT_REACHED");

        var book = send(get("/api/books/{id}", fourth).with(user1()));
        assertThat(read(book, "$.data.available_copies", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("When member request tries to borrow a book and has an overdue loan, must be rejected")
    void borrowIsRejectedWhenTheMemberHasAnOverdueLoan() throws Exception {
        populateMember("Razza", "razza@razza.com");
        long firstBook = populateBook("First", "isbn-first", 1);
        long secondBook = populateBook("Second", "isbn-second", 1);
        long loanId = borrow(firstBook);

        // make the loan overdue
        var loan = loanRepository.findById(loanId).orElseThrow();
        loan.setDueDate(Instant.now().minusSeconds(60));
        loanRepository.save(loan);

        var response = send(post("/api/loans")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(secondBook)));

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(code(response)).isEqualTo("MEMBER_HAS_OVERDUE_LOAN");

        var book = send(get("/api/books/{id}", secondBook).with(user1()));
        assertThat(read(book, "$.data.available_copies", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("When member request tries to return a book, must restore a copy and a second failed")
    void returnRestoresACopyAndASecondReturnFailed() throws Exception {
        populateMember("Razza", "razza@razza.com");

        long bookId = populateBook("Return", "isbn-return", 1);
        long loanId = borrow(bookId);

        var returned = send(post("/api/loans/{id}/return", loanId).with(user1()));

        assertThat(returned.getStatus()).isEqualTo(200);
        assertThat(read(returned, "$.data.returned_at", String.class)).isNotNull();
        var restored = send(get("/api/books/{id}", bookId).with(user1()));
        assertThat(read(restored, "$.data.available_copies", Integer.class)).isEqualTo(1);

        var again = send(post("/api/loans/{id}/return", loanId).with(user1()));

        assertThat(again.getStatus()).isEqualTo(422);
        assertThat(code(again)).isEqualTo("LOAN_ALREADY_RETURNED");
        var unchanged = send(get("/api/books/{id}", bookId).with(user1()));
        assertThat(read(unchanged, "$.data.available_copies", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("When two members try to borrow a book concurrently, only one of them gets the last copy")
    void onlyOneOfTwoConcurrentBorrowsGetsTheLastCopy() throws Exception {
        populateMember("Razza", "razza@razza.com");
        populateMember("Rafif", "rafif@rafif.com");

        long bookId = populateBook("Last", "isbn-last", 1);
        CyclicBarrier barrier = new CyclicBarrier(2);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<MockHttpServletResponse>> tasks = List.of(
                () -> borrowAfter(user1(), bookId, barrier),
                () -> borrowAfter(user2(), bookId, barrier));

            var responses = pool.invokeAll(tasks, 10, TimeUnit.SECONDS);

            MockHttpServletResponse first = responses.get(0).get();
            MockHttpServletResponse second = responses.get(1).get();
            long created = countStatus(201, first, second);
            long unavailable = countCode("BOOK_UNAVAILABLE", first, second);

            assertThat(created).isEqualTo(1);
            assertThat(unavailable).isEqualTo(1);

            var book = send(get("/api/books/{id}", bookId).with(admin()));
            assertThat(read(book, "$.data.available_copies", Integer.class)).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    private long borrow(long bookId) throws Exception {
        var response = send(post("/api/loans")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(bookId)));
        assertThat(response.getStatus()).isEqualTo(201);
        return read(response, "$.data.id", Number.class).longValue();
    }

    private MockHttpServletResponse borrowAfter(
            RequestPostProcessor user,
            long bookId,
            CyclicBarrier barrier
    ) throws Exception {
        barrier.await(5, TimeUnit.SECONDS);
        return send(post("/api/loans")
                .with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loanBody(bookId)));
    }

    private static String loanBody(long bookId) {
        return "{\"book_id\":%d}".formatted(bookId);
    }

    private static long countStatus(int status, MockHttpServletResponse... responses) {
        long count = 0;
        for (MockHttpServletResponse response : responses) {
            if (response.getStatus() == status) {
                count++;
            }
        }
        return count;
    }

    private static long countCode(String expected, MockHttpServletResponse... responses) throws Exception {
        long count = 0;
        for (MockHttpServletResponse response : responses) {
            if (response.getStatus() != 201 && expected.equals(code(response))) {
                count++;
            }
        }
        return count;
    }
}
