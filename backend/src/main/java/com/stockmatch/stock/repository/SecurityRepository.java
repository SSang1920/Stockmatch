package com.stockmatch.stock.repository;

import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecurityRepository extends JpaRepository<Security, Long>, SecurityRepositoryCustom {

    Optional<Security> findByTicker(String ticker);
    Optional<Security> findByTickerAndMarket(String ticker, Market market);
    long countByMarket(Market market);
    List<Security> findByTickerIn(List<String> tickers);
}
