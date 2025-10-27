package com.stockmatch.config.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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

        // Principal을 customUserDetails 타입으로 변환
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // 우리의 user 엔티티를 직접 가져옴
        User user = userDetails.getUser();

        // User 엔티티의 Long  타입 id를 문자열로 변환하여 userPk로 사용
        String userPk = String.valueOf(user.getId());
        String role = user.getRoleKey();

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
