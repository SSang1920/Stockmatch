package com.stockmatch.controller;

import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshAccessToken(
            @RequestHeader("Authorization") String refreshTokenHeader) {

        String refreshToken = resolveToken(refreshTokenHeader);

        // refreshToken 유효성 검사
        jwtUtil.validateTokenOrThrow(refreshToken);

        // userPk 추출
        String userPk = jwtUtil.getUserPkFromToken(refreshToken);

        // 사용자 정보 조회
        User user = userRepository.findById(Long.parseLong(userPk))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 새 accessToken 생성
        String newAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());

        log.info("Access Token 재발급 성공. userPk: {}", userPk);

        // responseData에 담아 반환
        Map<String, String> responseData = new HashMap<>();
        responseData.put("accessToken", newAccessToken);

        return ResponseEntity.ok(ApiResponse.ok(responseData));
    }

    private String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // 토큰이 없거나 형식이 잘못된 경우 예외 발생
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

}
