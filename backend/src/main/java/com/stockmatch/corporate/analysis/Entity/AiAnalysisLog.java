package com.stockmatch.corporate.analysis.Entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AiAnalysisLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String symbol;

    // AI 응답 결과를 통째로 저장
    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiResponseJson;

    @CreatedDate
    @Column(nullable = false)
    private LocalDateTime analyzedAt;

    @Builder
    public AiAnalysisLog(Long userId, String symbol, String aiResponseJson){
        this.userId = userId;
        this.symbol = symbol;
        this.aiResponseJson = aiResponseJson;
    }
}
