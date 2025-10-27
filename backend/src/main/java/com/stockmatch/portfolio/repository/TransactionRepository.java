package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    long countByPortfolioId(Long portfolioId);
}
