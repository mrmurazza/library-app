package com.lexhive.libraryapp.exception;

import org.springframework.http.HttpStatus;

public class CustomException extends RuntimeException {

    public enum Code {
        // Load-Related Errors
        LOAN_NOT_FOUND,
        INVALID_LOAN_STATUS,
        LOAN_LIMIT_REACHED,
        LOAN_ALREADY_RETURNED,

        // Book-Related Errors
        BOOK_NOT_FOUND,
        BOOK_UNAVAILABLE,
        BOOK_HAS_ACTIVE_LOANS,
        BOOK_HAS_LOAN_HISTORY,
        ISBN_ALREADY_EXISTS,
        COPIES_BELOW_LOANED,

        // Member-Related Errors
        MEMBER_NOT_FOUND,
        MEMBER_HAS_OVERDUE_LOAN,
        MEMBER_ACCOUNT_NOT_LINKED,
        EMAIL_ALREADY_EXISTS,

        // General Errors
        VALIDATION_ERROR,
        DATA_CONFLICT,
        FORBIDDEN,
        UNAUTHORIZED,
        INTERNAL_ERROR,
    }

    private final HttpStatus status;
    private final Code code;

    public CustomException(HttpStatus status, Code code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public Code code() {
        return code;
    }

    public static CustomException notFound(Code code, String message) {
        return new CustomException(HttpStatus.NOT_FOUND, code, message);
    }

    public static CustomException unprocessableEntity(Code code, String message) {
        return new CustomException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public static CustomException badRequest(Code code, String message) {
        return new CustomException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static CustomException forbidden(Code code, String message) {
        return new CustomException(HttpStatus.FORBIDDEN, code, message);
    }
}
