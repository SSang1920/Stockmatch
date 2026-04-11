package com.stockmatch.corporate.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.corporate.analysis.Entity.AiAnalysisLog;
import com.stockmatch.corporate.analysis.Entity.AnalysisType;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
import com.stockmatch.corporate.analysis.dto.response.AnalysisHistoryListResponse;
import com.stockmatch.corporate.analysis.repository.AiAnalysisLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiHistoryService {
    private final AiAnalysisLogRepository logRepository;
    private final ObjectMapper objectMapper;

    public void saveAnalysisLog(Long userId, AnalysisType type, String symbol, String companyName , AiResponseDto dto){
        this.saveAnalysisLog(userId, type, symbol, companyName, null, dto);
    }

    public void saveAnalysisLog(Long userId, AnalysisType type, String symbol, String companyName, String userComment, AiResponseDto dto){
        try{
            String jsonResult = objectMapper.writeValueAsString(dto);

            String displaySymbol = (type == AnalysisType.PORTFOLIO) ? "포트폴리오 분석" : companyName + " (" + symbol + ")";

            AiAnalysisLog logEntity = AiAnalysisLog.builder()
                    .userId(userId)
                    .analysisType(type)
                    .symbol(displaySymbol)
                    .userComment(userComment)
                    .aiResponseJson(jsonResult)
                    .build();

            logRepository.save(logEntity);

        } catch (Exception e){
            log.error("AI 분석 기록 저장중 오류 발생");
        }
    }

    public List<AnalysisHistoryListResponse> getHistoryList(long userId, AnalysisType type) {
        List<AiAnalysisLog> logs ;

        if (type == null) {
            logs = logRepository.findAllByUserIdOrderByAnalyzedAtDesc(userId);
        } else {
            logs = logRepository.findAllByUserIdAndAnalysisTypeOrderByAnalyzedAtDesc(userId, type);
        }

        return logs.stream()
                .map(logEntity -> AnalysisHistoryListResponse.builder()
                        .id(logEntity.getId())
                        .symbol(logEntity.getSymbol())
                        .analyzedAt(logEntity.getAnalyzedAt())
                        .type(logEntity.getAnalysisType())
                        .userComment(logEntity.getUserComment())
                        .build())
                .toList();
    }

    public AiResponseDto getHistoryDetail(Long id) {
        AiAnalysisLog logEntity = logRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DART_CORP_CODE_NOT_FOUNT));

        try{
            return objectMapper.readValue(logEntity.getAiResponseJson(), AiResponseDto.class);
        } catch (Exception e){
            log.error("상세 데이터 파싱 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_JSON_FORMAT);
        }
    }
}
