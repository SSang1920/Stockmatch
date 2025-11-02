package com.stockmatch.user.member.repository;

import com.stockmatch.user.member.domain.AlphaVantageKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlphaVantageKeyRepository extends JpaRepository<AlphaVantageKey, Long> {

    Optional<AlphaVantageKey> findByUserId(Long userId);
}
