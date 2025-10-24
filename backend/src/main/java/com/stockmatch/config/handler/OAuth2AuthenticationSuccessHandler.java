package com.stockmatch.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.config.jwt.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtutil;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException{

        //사용자 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // User 엔티티의 PK(id)
        String userPk = oAuth2User.getName();

        //사용자 권한 정보 추출
        String role = oAuth2User.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No authorities found"))
                .getAuthority();

        // JWT 토큰 생성
        String accessToken = jwtutil.generateAccessToken(userPk, role);
        String refreshToken = jwtutil.generateRefreshToken(userPk);

        log.info("OAuth2 로그인 성공. Access/Refresh Token 생성 완료. userPk: {}", userPk);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", "OAuth2 로그인 및 JWT 토큰 발급 성공");
        responseBody.put("accessToken", accessToken);
        responseBody.put("refreshToken", refreshToken);

        // ObjectMapper를 사용하여 Map을 JSON 문자열로 변환
        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}
