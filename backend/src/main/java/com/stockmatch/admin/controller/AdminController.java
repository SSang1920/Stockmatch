package com.stockmatch.admin.controller;

import com.stockmatch.admin.dto.AdminDashboardResponseDto;
import com.stockmatch.admin.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponseDto> getDashboardStats() {
        return ResponseEntity.ok(adminDashboardService.getDailyStatistics());
    }
}
