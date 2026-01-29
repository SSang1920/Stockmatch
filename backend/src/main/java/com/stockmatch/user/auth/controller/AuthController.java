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

    @GetMapping("/callback/{provider}")
    public void callback(
            @PathVariable String provider,
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletResponse response
    ) throws IOException {

        Map<String, String> tokens = authService.login(provider, code, state);

        String accessToken = tokens.get("accessToken");
        String refreshToken = tokens.get("refreshToken");

        // accessToken 쿠키 생성
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .path("/")
                .httpOnly(false) //프론트에서 읽을시 false
                .secure(false) // 로컬 환경 false, 배포 환경 true
                .sameSite("Lax") // 타 도메인간 리다이렉트 시 쿠키 전달 허용
                .maxAge(60*60)
                .build();

        //refreshToken 쿠키 생성
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .httpOnly(true) // refreshToken은 서버만 읽도록
                .secure(false)
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect("http://localhost:5173");
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refreshAccessToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        TokenResponseDto responseData = authService.refreshAccessToken(refreshToken);

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        authService.logout(userId);

        ResponseCookie expiredAccess = ResponseCookie.from("accessToken", "").path("/").maxAge(0).build();
        ResponseCookie expiredRefresh = ResponseCookie.from("refreshToken", "").path("/").httpOnly(true).maxAge(0).build();

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
