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
import com.lexhive.libraryapp.repository.LoanRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookService(BookRepository bookRepository, LoanRepository loanRepository) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> list() {
        return bookRepository.findAllByOrderByTitleAscIdAsc().stream().map(BookResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(long id) {
        return BookResponse.fromEntity(findBook(id));
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        String isbn = request.isbn().trim();
        if (bookRepository.existsByIsbn(isbn)) {
            throw CustomException.unprocessableEntity(Code.ISBN_ALREADY_EXISTS, String.format("ISBN %s is already in the catalog", isbn));
        }
        Book book = new Book(request.title().trim(), request.author().trim(), isbn, request.totalCopies());
        return BookResponse.fromEntity(bookRepository.save(book));
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
                throw CustomException.unprocessableEntity(Code.ISBN_ALREADY_EXISTS, String.format("ISBN %s is already in the catalog", isbn));
            }
            book.setIsbn(isbn);
        }

        if (request.totalCopies() != null && request.totalCopies() != book.getTotalCopies()) {
            int onLoan = book.getTotalCopies() - book.getAvailableCopies();
            int available = book.getAvailableCopies() + (request.totalCopies() - book.getTotalCopies());
            if (available < 0) {
                throw CustomException.unprocessableEntity(
                        Code.COPIES_BELOW_LOANED,
                        String.format("Cannot set totalCopies to %d because %d copies are on loan", request.totalCopies(), onLoan));
            }

            book.setTotalCopies(request.totalCopies());
            book.setAvailableCopies(available);
        }
        return BookResponse.fromEntity(book);
    }

    @Transactional
    public void deleteBook(long id) {
        Book book = lockBook(id);
        if (loanRepository.existsByBook_IdAndReturnedAtIsNull(id)) {
            throw CustomException.unprocessableEntity(Code.BOOK_HAS_ACTIVE_LOANS, String.format("Book %d still has an active loan", id));
        }

        if (loanRepository.existsByBook_Id(id)) {
            throw CustomException.unprocessableEntity(Code.BOOK_HAS_LOAN_HISTORY, String.format("Book %d has loan history and cannot be deleted", id));
        }

        bookRepository.delete(book);
    }

    private Book findBook(long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound(Code.BOOK_NOT_FOUND, String.format("Book %d was not found", id)));
    }

    private Book lockBook(long id) {
        return bookRepository.findByIdForUpdate(id)
                .orElseThrow(() -> CustomException.notFound(Code.BOOK_NOT_FOUND, String.format("Book %d was not found", id)));
    }
}
