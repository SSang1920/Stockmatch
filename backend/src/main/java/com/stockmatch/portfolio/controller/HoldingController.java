package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.HoldingRequest;
import com.stockmatch.portfolio.dto.HoldingResponse;
import com.stockmatch.portfolio.service.HoldingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio/me/holding")
public class HoldingController {

    private HoldingService holdingService;

    @PostMapping
    public ResponseEntity<ApiResponse<HoldingResponse>> addHolding(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody HoldingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(holdingService.addHolding(user.getUser().getId(), request)));
    }
}
