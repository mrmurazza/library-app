package com.lexhive.libraryapp.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lexhive.libraryapp.dto.BookResponse;
import com.lexhive.libraryapp.dto.CreateBookRequest;
import com.lexhive.libraryapp.dto.UpdateBookRequest;
import com.lexhive.libraryapp.entity.Book;
import com.lexhive.libraryapp.exception.CustomException;
import com.lexhive.libraryapp.exception.CustomException.Code;
import com.lexhive.libraryapp.repository.BookRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> list() {
        return bookRepository.findAllByOrderByTitleAscIdAsc().stream()
                .map(BookResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(long id) {
        return BookResponse.from(findBook(id));
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw CustomException.conflict(Code.ISBN_ALREADY_EXISTS, "ISBN " + isbn + " is already in the catalog");
        }
        Book book = new Book(request.title().trim(), request.author().trim(), isbn, request.totalCopies());
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateBook(long id, UpdateBookRequest request) {
        Book book = lockBook(id);
        if (request.title() != null) {
            book.setTitle(request.title().trim());
        }
        if (request.author() != null) {
            book.setAuthor(request.author().trim());
        }
        if (request.isbn() != null) {
            String isbn = request.isbn().trim();
            if (bookRepository.existsByIsbnAndIdNot(isbn, id)) {
                throw CustomException.conflict(Code.ISBN_ALREADY_EXISTS, "ISBN " + isbn + " is already in the catalog");
            }
            book.setIsbn(isbn);
        }
        if (request.totalCopies() != null && request.totalCopies() != book.getTotalCopies()) {
            int onLoan = book.getTotalCopies() - book.getAvailableCopies();
            int available = book.getAvailableCopies() + (request.totalCopies() - book.getTotalCopies());
            if (available < 0) {
                throw CustomException.conflict(
                        Code.COPIES_BELOW_LOANED,
                        "Cannot set totalCopies to " + request.totalCopies() + " because " + onLoan + " copies are on loan");
            }
            book.setTotalCopies(request.totalCopies());
            book.setAvailableCopies(available);
        }
        return BookResponse.from(book);
    }

    @Transactional
    public void deleteBook(long id) {
        Book book = lockBook(id);
        // TODO: Check if book has any loans
        bookRepository.delete(book);
    }

    private Book findBook(long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(Code.BOOK_NOT_FOUND, "Book " + id + " was not found"));
    }

    private Book lockBook(long id) {
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> CustomException.notFound(Code.BOOK_NOT_FOUND, "Book " + id + " was not found"));
    }
}
