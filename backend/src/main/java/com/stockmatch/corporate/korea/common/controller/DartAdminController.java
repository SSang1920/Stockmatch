package com.stockmatch.corporate.korea.common.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.corporate.korea.common.service.DartSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dart")
@RequiredArgsConstructor
public class DartAdminController {

    private final DartSyncService dartSyncService;

    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<String>> syncDartClass() {
        dartSyncService.syncCorpCodes();

        return ResponseEntity.ok(ApiResponse.ok("DART 고유번호 동기화 완료"));
    }
}
