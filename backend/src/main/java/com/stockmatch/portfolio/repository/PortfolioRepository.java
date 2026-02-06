package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Portfolio;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM Portfolio p WHERE p.user.id IN : userIds")
    void deleteAllByUserIds(@Param("userIds")List<Long> userIds);

    // 오늘 거래가 된 포트폴리오의 숫자를 셈
    @Query("SELECT COUNT(DISTINCT p) FROM Portfolio p JOIN p.transactions t WHERE t.createdAt BETWEEN :start And :end")
    Long countTodayActivePortfolios(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
