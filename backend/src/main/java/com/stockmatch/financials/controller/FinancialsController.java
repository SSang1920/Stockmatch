package com.stockmatch.financials.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.financials.dto.CompanyOverviewResponse;
import com.stockmatch.financials.infra.AlphaVantageClient;
import com.stockmatch.financials.service.FinancialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/financials")
@RequiredArgsConstructor
public class FinancialsController {

    private final FinancialService financialService;

    /**
     * 기업의 개요 정보 조회
     * @param userDetails 로그인한 사용자의 정보
     * @param symbol 조회할 기업 티커
     * @return 기업 개요 데이터
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<CompanyOverviewResponse>> getCompanyOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        var data = financialService.getCompanyOverview(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

}
