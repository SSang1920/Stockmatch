package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.domain.Transaction;
import com.stockmatch.stock.domain.Security;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Slice<Transaction> findByPortfolioIdOrderByTradeAtDesc(Long portfolioId, Pageable pageable);

    List<Transaction> findAllByPortfolioIdOrderByTradeAtAsc(Long portfolioId);

    List<Transaction> findByPortfolioAndSecurityOrderByTradeAtAsc(Portfolio portfolio, Security security);
}
