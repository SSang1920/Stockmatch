package com.stockmatch.corporate.analysis.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
import com.stockmatch.corporate.analysis.dto.response.AnalysisHistoryListResponse;
import com.stockmatch.corporate.analysis.dto.response.AnalysisHistoryResponse;
import com.stockmatch.corporate.analysis.service.AiAnalysisService;
import com.stockmatch.corporate.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/{symbol}")
    public ResponseEntity<ApiResponse<AnalysisPackage>> getAnalysisReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage analysisPackage = analysisService.analyzeStock(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(analysisPackage));
    }

    @GetMapping("/{symbol}/ai")
    public ResponseEntity<ApiResponse<AiResponseDto>> getAiInvestmentAdvice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage analysisPackage = analysisService.analyzeStock(userId, symbol);

        AiResponseDto aiResult = aiAnalysisService.getInvestmentAdvice(userId, symbol, analysisPackage);


        return ResponseEntity.ok(ApiResponse.ok(aiResult));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AnalysisHistoryListResponse>>> getHistoryList(
            @AuthenticationPrincipal CustomUserDetails userDetails) {


        return ResponseEntity.ok(ApiResponse.ok(aiAnalysisService.getHistoryList(userDetails.getUser().getId())));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<AiResponseDto>> getHistoryDetail(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(aiAnalysisService.getHistoryDetail(id)));
    }

}
