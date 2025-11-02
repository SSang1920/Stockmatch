package com.stockmatch.user.member.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.member.dto.request.ApiKeyRequest;
import com.stockmatch.user.member.dto.response.UserInfoResponse;
import com.stockmatch.user.member.dto.request.UserProfileUpdateRequest;
import com.stockmatch.user.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUser().getId();
        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
