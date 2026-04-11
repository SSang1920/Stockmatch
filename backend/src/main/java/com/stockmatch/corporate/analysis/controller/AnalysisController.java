package com.stockmatch.corporate.analysis.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.analysis.Entity.AnalysisType;
import com.stockmatch.corporate.analysis.dto.data.AnalysisPackage;
import com.stockmatch.corporate.analysis.dto.request.PortfolioRequestDto;
import com.stockmatch.corporate.analysis.dto.response.AiResponseDto;
import com.stockmatch.corporate.analysis.dto.response.AnalysisHistoryListResponse;
import com.stockmatch.corporate.analysis.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final AiAnalysisService aiAnalysisService;
    private final AiHistoryService aiHistoryService;
    private final AiFinancialAnalysisService aiFinancialAnalysisService;
    private final AiPortfolioAnalysisService aiPortfolioAnalysisService;

    @GetMapping("/{symbol}")
    public ResponseEntity<ApiResponse<AnalysisPackage>> getAnalysisReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage analysisPackage = analysisService.analyzeStock(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(analysisPackage));
    }

    @GetMapping("/{symbol}/stock-ai")
    public ResponseEntity<ApiResponse<AiResponseDto>> getAiInvestmentAdvice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage analysisPackage = analysisService.analyzeStock(userId, symbol);

        AiResponseDto aiResult = aiAnalysisService.getInvestmentAdvice(userId, symbol, analysisPackage);


        return ResponseEntity.ok(ApiResponse.ok(aiResult));
    }

    @GetMapping("/{symbol}/financial-ai")
    public ResponseEntity<ApiResponse<AiResponseDto>> getAiFinancialAdvice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage originPackage = analysisService.analyzeStock(userId, symbol);

        // 유저의 포트폴리오, 투자 성향부분 생략 (토큰 절약)
        AnalysisPackage financialPackage = AnalysisPackage.builder()
                // .user() 필드는 의도적으로 생략 (null)
                .targetStock(originPackage.getTargetStock())
                .businessPerformance(originPackage.getBusinessPerformance())
                .marketMomentum(originPackage.getMarketMomentum())
                .financialHealth(originPackage.getFinancialHealth())
                .missingData(originPackage.getMissingData())
                .build();

        AiResponseDto aiResult = aiFinancialAnalysisService.getFinancialAdvice(userId, symbol, financialPackage);

        return ResponseEntity.ok(ApiResponse.ok(aiResult));
    }

    @PostMapping("/portfolio-ai")
    public ResponseEntity<ApiResponse<AiResponseDto>> getAiPortfolioAdvice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PortfolioRequestDto request) {

        Long userId = userDetails.getUser().getId();

        //내 포트폴리오 가져오기
        AnalysisPackage portfolioPackage = analysisService.analyzeMyPortfolio(userId);


        AiResponseDto aiResult = aiPortfolioAnalysisService.getPortfolioAdvice(userId, portfolioPackage, request.getComment());

        return ResponseEntity.ok(ApiResponse.ok(aiResult));
    }


    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AnalysisHistoryListResponse>>> getHistoryList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) AnalysisType type) {


        return ResponseEntity.ok(ApiResponse.ok(aiHistoryService.getHistoryList(userDetails.getUser().getId(), type)));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<ApiResponse<AiResponseDto>> getHistoryDetail(
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.ok(aiHistoryService.getHistoryDetail(id)));
    }

}
