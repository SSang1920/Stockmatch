package com.stockmatch.user.member.repository;

import com.stockmatch.user.member.domain.AlphaVantageKey;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlphaVantageKeyRepository extends JpaRepository<AlphaVantageKey, Long> {

    Optional<AlphaVantageKey> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM AlphaVantageKey a WHERE a.user.id IN : userIds")
    void deleteAllByUserIds(@Param("userIds") List<Long> userIds);
}
