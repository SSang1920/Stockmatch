package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Holding;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.stock.domain.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long>, HoldingRepositoryCustom {

    Optional<Holding> findByPortfolioIdAndSecurityId(Long portfolioId, Long securityId);

    Optional<Holding> findByPortfolioAndSecurity(Portfolio portfolio, Security security);
}
