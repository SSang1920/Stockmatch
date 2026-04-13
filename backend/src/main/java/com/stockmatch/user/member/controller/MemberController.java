package com.stockmatch.user.member.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.member.dto.request.ApiKeyRequest;
import com.stockmatch.user.member.dto.request.InvestmentResultRequest;
import com.stockmatch.user.member.dto.response.UserInfoResponse;
import com.stockmatch.user.member.dto.request.UserProfileUpdateRequest;
import com.stockmatch.user.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

                Long userId = userDetails.getUser().getId();
                UserInfoResponse userInfo = userService.getUserInfo(userId);

                return ResponseEntity.ok(ApiResponse.ok(userInfo));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserProfileUpdateRequest request){

        Long userId = userDetails.getUser().getId();
        userService.updateUserProfile(userId,request);

        return ResponseEntity.ok(ApiResponse.ok());
    }

    /**
     * api Vantage 키 등록 수정
     */
    @PostMapping("/me/api-key")
    public ResponseEntity<ApiResponse<Void>> upsertApiKey(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ApiKeyRequest request) {

        Long userId = userDetails.getUser().getId();
        userService.upsertAlphaVantageKey(userId, request.getApiKey());

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/me/api-key/decrypted")
    public ResponseEntity<ApiResponse<String>> getDecryptedApiKey(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        String realKey = userService.getDecryptedApiKey(userId);

        return ResponseEntity.ok(ApiResponse.ok(realKey));
    }


    @PostMapping("me/investment-profile")
    public ResponseEntity<Void> registerInvestmentProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid InvestmentResultRequest request){

        if (userDetails == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = userDetails.getUser().getId();

        userService.registerInvestmentProfile(userId, request);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @CookieValue(name = "refreshToken", required=false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Long userId = userDetails.getUser().getId();
        userService.deleteUser(userId);

        ResponseCookie expiredAccess = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();

        ResponseCookie expiredRefresh = ResponseCookie.from("refreshToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .body(ApiResponse.ok());
    }
}
