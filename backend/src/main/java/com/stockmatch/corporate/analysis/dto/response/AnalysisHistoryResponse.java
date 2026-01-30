package com.stockmatch.corporate.analysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class AnalysisHistoryResponse {
    private Long id;
    private String symbol;
    private AiResponseDto analysis;
    private LocalDateTime analyzedAt;
}
