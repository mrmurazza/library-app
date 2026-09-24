package com.lexhive.libraryapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class BookControllerTest extends ControllerIntegrationTest {

    @Test
    @DisplayName("When anonymous request is unauthorized")
    void anonymousRequestIsUnauthorized() throws Exception {
        var response = send(get("/api/books"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(code(response)).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("When member request tries to create a book, must got forbidden")
    void memberCannotCreateABook() throws Exception {
        var response = send(post("/api/books")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Dune","author":"Frank Herbert","isbn":"9780441172719","total_copies":1}
                        """));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(code(response)).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("When member request tries to list the catalog, must be successful")
    void memberCanListTheCatalog() throws Exception {
        populateBook("Kindred", "9780807083697", 2);

        var response = send(get("/api/books").with(user1()));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(read(response, "$.length()", Integer.class)).isEqualTo(1);
        assertThat(read(response, "$[0].isbn", String.class)).isEqualTo("9780807083697");
    }

    @Test
    @DisplayName("When admin request tries to create a book, must be successful")
    void adminCanCreateABook() throws Exception {
        var response = send(post("/api/books")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Dune","author":"Frank Herbert","isbn":"9780441172719","total_copies":3}
                        """));

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(read(response, "$.title", String.class)).isEqualTo("Dune");
        assertThat(read(response, "$.isbn", String.class)).isEqualTo("9780441172719");
        assertThat(read(response, "$.total_copies", Integer.class)).isEqualTo(3);
        assertThat(read(response, "$.available_copies", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("When admin request tries to update a book, must be successful")
    void adminCanUpdateABook() throws Exception {
        long bookId = populateBook("Dune", "9780441172719", 3);

        var response = send(patch("/api/books/{id}", bookId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Dune Messiah","author":"Frank Herbert"}
                        """));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(read(response, "$.title", String.class)).isEqualTo("Dune Messiah");
        assertThat(read(response, "$.author", String.class)).isEqualTo("Frank Herbert");
        assertThat(read(response, "$.total_copies", Integer.class)).isEqualTo(3);
    }

    @Test
    @DisplayName("When admin request tries to delete a book, must be successful")
    void adminCanDeleteABook() throws Exception {
        long bookId = populateBook("Dune", "9780441172719", 1);

        var deleted = send(delete("/api/books/{id}", bookId).with(admin()));

        assertThat(deleted.getStatus()).isEqualTo(204);

        var missing = send(get("/api/books/{id}", bookId).with(admin()));
        assertThat(missing.getStatus()).isEqualTo(404);
        assertThat(code(missing)).isEqualTo("BOOK_NOT_FOUND");
    }

    @Test
    @DisplayName("When admin request tries to create a book with duplicate ISBN, must be rejected")
    void duplicateIsbnIsRejected() throws Exception {
        populateBook("Kindred", "9780807083697", 1);

        var response = send(post("/api/books")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Another","author":"Author","isbn":"9780807083697","total_copies":1}
                        """));

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(code(response)).isEqualTo("ISBN_ALREADY_EXISTS");
    }

    @Test
    @DisplayName("When admin request tries to lower the total copies below the copies on loan, must be rejected")
    void totalCopiesCannotDropBelowCopiesOnLoan() throws Exception {
        populateMember("Razza", "razza@razza.com");

        long bookId = populateBook("Stock", "isbn-stock", 3);
        borrow(bookId);
        borrow(bookId);

        var lowered = send(patch("/api/books/{id}", bookId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"total_copies":2}
                        """));

        assertThat(lowered.getStatus()).isEqualTo(200);
        assertThat(read(lowered, "$.total_copies", Integer.class)).isEqualTo(2);
        assertThat(read(lowered, "$.available_copies", Integer.class)).isZero();

        var rejected = send(patch("/api/books/{id}", bookId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"total_copies":1}
                        """));

        assertThat(rejected.getStatus()).isEqualTo(422);
        assertThat(code(rejected)).isEqualTo("COPIES_BELOW_LOANED");

        var current = send(get("/api/books/{id}", bookId).with(admin()));
        assertThat(read(current, "$.total_copies", Integer.class)).isEqualTo(2);
    }

    private void borrow(long bookId) throws Exception {
        var response = send(post("/api/loans")
                .with(user1())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"book_id":%d}
                        """.formatted(bookId)));
        assertThat(response.getStatus()).isEqualTo(201);
    }
}
