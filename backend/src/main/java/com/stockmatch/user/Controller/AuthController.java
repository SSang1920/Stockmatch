package com.stockmatch.user.Controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshAccessToken(
            @RequestParam("Authorization") String refreshTokenHeader) {

        Map<String, String> responseData = authService.refreshAccessToken(refreshTokenHeader);

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @GetMapping("/callback/kakao")
    public ResponseEntity<ApiResponse<Map<String, String>>> kakaoCallback(
            @RequestParam("code") String code) {
        log.info("kakao login callback code: {}", code);
        Map<String, String> tokens = authService.kakaoLogin(code);

        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @GetMapping("/callback/naver")
    public ResponseEntity<ApiResponse<Map<String, String>>> naverCallback(
            @RequestParam("code") String code, @RequestParam("state") String state) {
        log.info("Naver login callback code: {}, state: {}", code, state);
        Map<String, String> tokens = authService.naverLogin(code, state);
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @GetMapping("/callback/google")
    public ResponseEntity<ApiResponse<Map<String, String>>> googleCallback(
            @RequestParam("code") String code) {
        log.info("Google login callback code: {}", code);
        Map<String, String> tokens = authService.googleLogin(code);
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }
}
