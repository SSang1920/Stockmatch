package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Holding;

import java.util.List;

public interface HoldingRepositoryCustom {
    List<Holding> findAllWithSecurityByPortfolioId(Long portfolioId);
}
