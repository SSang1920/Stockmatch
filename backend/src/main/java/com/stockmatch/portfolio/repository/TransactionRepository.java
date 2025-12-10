package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByPortfolioIdOrderByTradeAtDesc(Long portfolioId);

    List<Transaction> findAllByPortfolioIdOrderByTradeAtAsc(Long portfolioId);
}
