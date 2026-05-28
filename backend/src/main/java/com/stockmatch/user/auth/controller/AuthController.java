package com.stockmatch.user.auth.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.auth.dto.TokenResponseDto;
import com.stockmatch.user.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontend-url:https://stockmatch.kro.kr}")
    private String frontendUrl;

    @GetMapping("/callback/{provider}")
    public void callback(
            @PathVariable String provider,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletResponse response
    ) throws IOException {

        if (error != null){
            response.sendRedirect(frontendUrl);
            return;
        }

        Map<String, String> tokens = authService.login(provider, code, state);

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        boolean isProd = !frontendUrl.contains("localhost");

        // accessToken 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .path("/")
                .httpOnly(true) //프론트에서 읽을시 false
                .secure(isProd) // 로컬 환경 false, 배포 환경 true
                .sameSite("None") // 타 도메인간 리다이렉트 시 쿠키 전달 허용
                .maxAge(60*60)
                .build();

        //refreshToken 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .httpOnly(true) // refreshToken은 서버만 읽도록
                .secure(isProd)
                .sameSite("None")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect(frontendUrl);
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        TokenResponseDto responseData = authService.refreshAccessToken(refreshToken);
        boolean isProd = !frontendUrl.contains("localhost");

        ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", responseData.getAccessToken())
                .path("/")
                .httpOnly(true)
                .secure(isProd)
                .sameSite("Lax")
                .maxAge(60 * 60)
                .build();

        response.addHeader("Set-Cookie", newAccessCookie.toString());

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        authService.logout(userId);

        boolean isProd = !frontendUrl.contains("localhost");

        ResponseCookie expiredAccess = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(isProd)
                .sameSite("None")
                .domain(isProd ? "stockmatch.kro.kr" : "localhost")
                .build();

        ResponseCookie expiredRefresh = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(isProd)
                .secure(true)
                .sameSite("None")
                .domain(isProd ? "stockmatch.kro.kr" : "localhost")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", expiredAccess.toString())
                .header("Set-Cookie", expiredRefresh.toString())
                .body(ApiResponse.ok());
    }

    /**
     * 로그인 여부 및 내 정보 확인
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }

        // 로그인 된 상태라면 간단한 유저 정보 반환
        Map<String, Object> userInfo = Map.of(
                "id", userDetails.getUser().getId(),
                "name", userDetails.getUser().getName(),
                "isAuthenticated", true
        );

        return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }
}
