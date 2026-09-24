package com.lexhive.libraryapp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lexhive.libraryapp.dto.CreateLoanRequest;
import com.lexhive.libraryapp.dto.LoanResponse;
import com.lexhive.libraryapp.dto.LoanStatus;
import com.lexhive.libraryapp.authentication.CurrentUserResolver;
import com.lexhive.libraryapp.service.LoanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Loans")
public class LoanController {

    private final LoanService loanService;
    private final CurrentUserResolver currentUserResolver;

    public LoanController(LoanService loanService, CurrentUserResolver currentUserResolver) {
        this.loanService = loanService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    @Operation(summary = "Borrow a book")
    public ResponseEntity<LoanResponse> creatBookLoan(@Valid @RequestBody CreateLoanRequest request, Authentication authentication) {
        LoanResponse body = loanService.createLoan(request.bookId(), currentUserResolver.from(authentication));

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "Return a loan")
    public LoanResponse returnLoan(@PathVariable long id, Authentication authentication) {
        return loanService.returnLoan(id, currentUserResolver.from(authentication));
    }

    @GetMapping
    @Operation(summary = "List loans. status=active includes overdue loans. Members only see their own.")
    public List<LoanResponse> list(@RequestParam(required = false) LoanStatus status, Authentication authentication) {
        return loanService.list(status, currentUserResolver.from(authentication));
    }
}
