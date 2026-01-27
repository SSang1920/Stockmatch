package com.stockmatch.corporate.analysis.repository;

import com.stockmatch.corporate.analysis.Entity.AiAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiAnalysisLogRepository extends JpaRepository<AiAnalysisLog, Long> {

    List<AiAnalysisLog> findAllByUserIdOrderByAnalyzedAtDesc(Long userId);
}
