package com.stockmatch.stock.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.stock.dto.SecurityRequest;
import com.stockmatch.stock.dto.SecurityResponse;
import com.stockmatch.stock.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/security")
public class SecurityController {

    private final SecurityService securityService;

    @PostMapping
    public ResponseEntity<ApiResponse<SecurityResponse>> createSecurity(
            @RequestBody SecurityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(securityService.createSecurity(request)));
    }
}
