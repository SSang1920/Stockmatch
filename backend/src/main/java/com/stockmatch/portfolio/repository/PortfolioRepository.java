package com.stockmatch.portfolio.repository;

import com.stockmatch.portfolio.domain.Portfolio;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM Portfolio p WHERE p.user.id IN : userIds")
    void deleteAllByUserIds(@Param("userIds")List<Long> userIds);


}
