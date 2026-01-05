package com.stockmatch.corporate.global.overview.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.global.overview.dto.CompanyOverviewDto;
import com.stockmatch.corporate.global.overview.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/corporate")
@RequiredArgsConstructor
public class OverviewController {

    private final OverviewService financialService;

    /**
     * 기업의 개요 정보 조회
     * @param userDetails 로그인한 사용자의 정보
     * @param symbol 조회할 기업 티커
     * @return 기업 개요 데이터
     */
    @GetMapping("/{symbol}/overview")
    public ResponseEntity<ApiResponse<CompanyOverviewDto>> getCompanyOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        var data = financialService.getCompanyOverview(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

}
