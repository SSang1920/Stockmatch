package com.stockmatch.stock.repository;

import com.stockmatch.stock.domain.DailyPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, Long> {

    /**
     * 특정 종목의 기간별 일일 시세를 날짜 오름차순 조회
     */
    List<DailyPrice> findBySecurityTickerAndDateBetweenOrderByDateAsc(String ticker, LocalDate from, LocalDate to);

    Optional<DailyPrice> findBySecurityIdAndDate(Long securityId, LocalDate date);

    @Query("select max(dp.date) from DailyPrice dp where dp.security.id = :securityId")
    LocalDate findMaxDateBySecurityId(@Param("securityId") Long securityId);

    List<DailyPrice> findBySecurityIdAndDateBetweenOrderByDateAsc(Long id, LocalDate from, LocalDate to);
}
