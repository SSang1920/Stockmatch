package com.stockmatch.corporate.incomestatement.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.incomestatement.dto.IncomeStatementDto;
import com.stockmatch.corporate.incomestatement.service.IncomeStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/corporate")
@RequiredArgsConstructor
public class IncomeStatementController {

    private final IncomeStatementService incomeStatementService;

    @GetMapping("/{symbol}/income")
    public ResponseEntity<ApiResponse<IncomeStatementDto>> getIncomeStatement(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("symbol") String symbol) {

        Long userId = userDetails.getUser().getId();

        var data  = incomeStatementService.getIncomeStatement(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(data));

    }
}
