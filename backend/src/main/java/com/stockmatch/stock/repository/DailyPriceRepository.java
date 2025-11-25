package com.stockmatch.stock.repository;

import com.stockmatch.stock.domain.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    /**
     * 특정 종목의 기간별 일일 시세를 날짜 오름차순 조회
     */
    List<DailyPrice> findBySecurityTickerAndDateBetweenOrderByDateAsc(String ticker, LocalDate from, LocalDate to);

    Optional<DailyPrice> findBySecurityIdAndDate(Long securityId, LocalDate date);
}
