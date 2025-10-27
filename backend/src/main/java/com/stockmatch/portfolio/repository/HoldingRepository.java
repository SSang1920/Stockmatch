package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    long countByPortfolioId(Long portfolioId);
}
