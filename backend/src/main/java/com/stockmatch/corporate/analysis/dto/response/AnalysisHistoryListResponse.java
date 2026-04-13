package com.stockmatch.corporate.analysis.dto.response;

import com.stockmatch.corporate.analysis.Entity.AnalysisType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHistoryListResponse {
    Long id;
    String symbol;
    LocalDateTime analyzedAt;
    private String userComment;

    private AnalysisType type;
}
