package com.lexhive.libraryapp.repository;

import com.lexhive.libraryapp.entity.Loan;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long>, LoanRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    List<Loan> findByMember_IdAndReturnedAtIsNull(Long memberId);

    boolean existsByBook_Id(Long bookId);

    boolean existsByBook_IdAndReturnedAtIsNull(Long bookId);
}
