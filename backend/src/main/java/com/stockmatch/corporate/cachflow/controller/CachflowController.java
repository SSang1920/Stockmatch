package com.stockmatch.corporate.cachflow.controller;


import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.cachflow.dto.CashflowDto;
import com.stockmatch.corporate.cachflow.service.CachflowService;
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
public class CachflowController {

    private final CachflowService cachflowService;

    @GetMapping("/{symbol}/cashflow")
    public ResponseEntity<ApiResponse<CashflowDto>> getCashflow(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        var data = cachflowService.getCachflow(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
