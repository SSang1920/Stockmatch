package com.stockmatch.corporate.korea.common.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DartCorpCodeRepository extends JpaRepository<DartCorpCode, String> {
    Optional<DartCorpCode> findByTicker(String ticker);
}
