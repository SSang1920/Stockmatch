package com.stockmatch.controller;

import com.stockmatch.config.security.CustomUserDetails;
import com.stockmatch.user.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증/인가 기능 테스트를 위한 임시 컨트롤러
 */
@RestController
@RequestMapping("/api")
public class TestController {

    /**
     * USER 또는 ADMIN 권한이 있어야 접근 가능한 API
     * SecurityConfig의 .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN") 규칙에 의해 보호됩니다.
     * @AuthenticationPrincipal을 통해 인증된 사용자 정보를 쉽게 가져올 수 있습니다.
     */
    @GetMapping("/user/hello")
    public ResponseEntity<String> userHello(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // @AuthenticationPrincipal 어노테이션이 SecurityContextHolder에서 현재 인증된 사용자의 Principal을 가져옵니다.
        // Principal은 우리가 JwtAuthenticationFilter에서 설정한 CustomUserDetails 객체입니다.
        User user = userDetails.getUser();
        String message = "Hello, " + user.getName() + "! Your role is " + user.getRoleKey() + ". You have access to USER area.";
        return ResponseEntity.ok(message);
    }

    /**
     * ADMIN 권한만 있어야 접근 가능한 API
     * SecurityConfig의 .requestMatchers("/api/admin/**").hasRole("ADMIN") 규칙에 의해 보호됩니다.
     */
    @GetMapping("/admin/hello")
    public ResponseEntity<String> adminHello(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String message = "Welcome, ADMIN " + userDetails.getUser().getName() + "!";
        return ResponseEntity.ok(message);
    }
}