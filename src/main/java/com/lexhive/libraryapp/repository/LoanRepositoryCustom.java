package com.lexhive.libraryapp.repository;

import java.util.List;

import com.lexhive.libraryapp.dto.LoanStatus;
import com.lexhive.libraryapp.entity.Loan;

public interface LoanRepositoryCustom {

    List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status);
}
