package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.PortfolioDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PortfolioDailySummaryRepository extends JpaRepository<PortfolioDailySummary, Long> {

    // 포트폴리오 ID와 기간으로 조회 후, 날짜순으로 정렬
    List<PortfolioDailySummary> findByPortfolioIdAndDateBetweenOrderByDateAsc(
            Long portfolioId,
            LocalDate from,
            LocalDate to
    );

    // 지정된 날짜 이전의 데이터 중 해당 월의 말이 아닌 일별 데이터 삭제
    @Modifying
    @Query(value = "DELETE FROM portfolio_daily_summary " +
            "WHERE date < :cutoffDate " +
            "AND date != LAST_DAY(date)",
            nativeQuery = true)
    int deleteOldDailyDataLeavingOnlyLastDayOfMonth(@Param("cutoffDate") LocalDate cutoffDate);
}
