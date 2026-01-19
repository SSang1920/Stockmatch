package com.stockmatch.corporate.analysis.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.analysis.dto.AnalysisPackage;
import com.stockmatch.corporate.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/{symbol}")
    public ResponseEntity<ApiResponse<AnalysisPackage>> getAnalysisReport(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        AnalysisPackage analysisPackage = analysisService.analyzeStock(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(analysisPackage));
    }
}
