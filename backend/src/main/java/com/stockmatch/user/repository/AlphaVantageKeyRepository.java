package com.stockmatch.user.repository;

import com.stockmatch.user.domain.AlphaVantageKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlphaVantageKeyRepository extends JpaRepository<AlphaVantageKey, Long> {

    Optional<AlphaVantageKey> findByUserId(Long userId);
}
