package com.lexhive.libraryapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lexhive.libraryapp.dto.ApiResponse;
import com.lexhive.libraryapp.dto.BookResponse;
import com.lexhive.libraryapp.dto.CreateBookRequest;
import com.lexhive.libraryapp.dto.UpdateBookRequest;
import com.lexhive.libraryapp.service.BookService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    @Operation(summary = "List the catalog")
    public ApiResponse<List<BookResponse>> GetBookCatalogList() {
        List<BookResponse> books = bookService.list();

        return ApiResponse.of(books);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one book")
    public ApiResponse<BookResponse> GetBook(@PathVariable long id) {
        BookResponse book = bookService.getBook(id);

        return ApiResponse.of(book);
    }

    @PostMapping
    @RolesAllowed("ADMIN")
    @Operation(summary = "Add a book")
    public ResponseEntity<ApiResponse<BookResponse>> addBook(@Valid @RequestBody CreateBookRequest request) {
        BookResponse created = bookService.createBook(request);
        ApiResponse<BookResponse> body = ApiResponse.of(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PatchMapping("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Update a book")
    public ApiResponse<BookResponse> updateBook(@PathVariable long id, @Valid @RequestBody UpdateBookRequest request) {
        BookResponse updated = bookService.updateBook(id, request);

        return ApiResponse.of(updated);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "Delete a book that has never been loaned")
    public ResponseEntity<Void> deleteBook(@PathVariable long id) {
        bookService.deleteBook(id);

        return ResponseEntity.noContent().build();
    }
}
