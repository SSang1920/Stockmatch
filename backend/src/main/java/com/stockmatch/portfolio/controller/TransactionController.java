package com.stockmatch.portfolio.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.portfolio.dto.TransactionCreateRequest;
import com.stockmatch.portfolio.dto.TransactionResponse;
import com.stockmatch.portfolio.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio/{portfolioId}/transaction")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Slice<TransactionResponse>>> getTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long portfolioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Slice<TransactionResponse> result =
                transactionService.getTransactions(userDetails.getUser().getId(), portfolioId, pageable);
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

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<ApiResponse<Void>>> deleteTransaction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long portfolioId,
            @PathVariable Long transactionId
    ) {
        transactionService.deleteTransaction(userDetails.getUser().getId(), portfolioId, transactionId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
