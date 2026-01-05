package com.stockmatch.corporate.global.earnings.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.corporate.global.earnings.dto.EarningsDto;
import com.stockmatch.corporate.global.earnings.service.EarningsService;
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
public class EarningsController {

    private final EarningsService earningsService;

    @GetMapping("/{symbol}/earnings")
    public ResponseEntity<ApiResponse<EarningsDto>> getEarnings(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("symbol") String symbol){

        Long userId = userDetails.getUser().getId();

        var data = earningsService.getEarnings(userId, symbol);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }


}
