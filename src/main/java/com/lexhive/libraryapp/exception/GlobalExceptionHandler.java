package com.lexhive.libraryapp.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.lexhive.libraryapp.dto.LoanStatus;
import com.lexhive.libraryapp.exception.CustomException.Code;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomException.class)
    ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        return ResponseEntity.status(ex.status()).body(ErrorResponse.fromErrorCode(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleUnauthenticated(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.fromErrorCode(Code.UNAUTHORIZED, "Authentication is required"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.fromErrorCode(Code.UNAUTHORIZED, "Authentication is required"));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.fromErrorCode(Code.FORBIDDEN, "You do not have access to this resource"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Request validation failed");

        return ResponseEntity.badRequest().body(ErrorResponse.fromErrorCode(Code.VALIDATION_ERROR, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        CustomException.Code code = ex.getRequiredType() == LoanStatus.class
                ? Code.INVALID_LOAN_STATUS
                : Code.VALIDATION_ERROR;

        return ResponseEntity.badRequest().body(ErrorResponse.fromErrorCode(code, "Invalid value for " + ex.getName()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("data conflict path={}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.fromErrorCode(Code.DATA_CONFLICT, "The request conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("unhandled error path={}", request.getRequestURI(), ex);

        return ResponseEntity.internalServerError()
                .body(ErrorResponse.fromErrorCode(Code.INTERNAL_ERROR, "Unexpected error"));
    }
}
