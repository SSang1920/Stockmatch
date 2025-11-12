package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long>, HoldingRepositoryCustom {

    List<Holding> findByPortfolioId(Long id);
}
