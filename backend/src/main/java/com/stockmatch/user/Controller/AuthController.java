package com.stockmatch.user.Controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.service.AuthService;
import com.stockmatch.user.service.UserService;
import com.stockmatch.user.service.unlink.OAuthUnlinker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/callback/{provider}")
    public ResponseEntity<ApiResponse<Map<String, String>>> callback(
            @PathVariable String provider,
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state) {

        log.info("{} login callback code: {}, state: {}", provider, code, state);

        Map<String, String> tokens = switch (provider.toLowerCase()) {
            case "kakao" -> authService.kakaoLogin(code);
            case "naver" -> authService.naverLogin(code, state);
            case "google" -> authService.googleLogin(code);
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };

        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshAccessToken(
            @RequestParam("Authorization") String refreshTokenHeader) {

        Map<String, String> responseData = authService.refreshAccessToken(refreshTokenHeader);

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
