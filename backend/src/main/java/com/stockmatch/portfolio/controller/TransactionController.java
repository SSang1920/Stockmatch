package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.TransactionCreateRequest;
import com.stockmatch.portfolio.dto.TransactionResponse;
import com.stockmatch.portfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio/{portfolioId}/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long portfolioId
    ) {
        List<TransactionResponse> result =
                transactionService.getTransactions(userDetails.getUser().getId(), portfolioId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<TransactionResponse>> buy(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long portfolioId,
            @RequestBody TransactionCreateRequest request
    ) {
        TransactionResponse result =
                transactionService.buy(userDetails.getUser().getId(), portfolioId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/sell")
    public ResponseEntity<ApiResponse<TransactionResponse>> sell(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long portfolioId,
            @RequestBody TransactionCreateRequest request
    ) {
        TransactionResponse result =
                transactionService.sell(userDetails.getUser().getId(), portfolioId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
