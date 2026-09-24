package com.lexhive.libraryapp.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.lexhive.libraryapp.dto.LoanStatus;
import com.lexhive.libraryapp.entity.Loan;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class LoanRepositoryCustomImpl implements LoanRepositoryCustom {

    private final EntityManager entityManager;

    public LoanRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Loan> findByMemberIdAndStatus(Long memberId, LoanStatus status) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Loan> query = builder.createQuery(Loan.class);
        Root<Loan> loan = query.from(Loan.class);

        List<Predicate> filters = new ArrayList<>();
        if (memberId != null) {
            filters.add(builder.equal(loan.get("member").get("id"), memberId));
        }

        if (status != null) {
            switch (status) {
                case ACTIVE -> filters.add(builder.isNull(loan.get("returnedAt")));
                case RETURNED -> filters.add(builder.isNotNull(loan.get("returnedAt")));
                case OVERDUE -> {
                    filters.add(builder.isNull(loan.get("returnedAt")));
                    filters.add(builder.lessThan(loan.get("dueDate"), Instant.now()));
                }
            }
        }

        if (!filters.isEmpty()) {
            query.where(filters.toArray(Predicate[]::new));
        }

        query.orderBy(builder.desc(loan.get("borrowedAt")), builder.desc(loan.get("id")));

        return entityManager.createQuery(query).getResultList();
    }
}
