package com.stockmatch.config;

import com.stockmatch.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //csrf 보호 비활성화 (JWT 사용 시)
                .csrf(csrf -> csrf.disable())


                // 세션 관리 정책 STETELESS 설정 (세션 사용X)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTML 폼 기반 로그인 비활성화
                .formLogin(form -> form.disable())

                // HTTP 기본 인증 기능 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())

                // HTTP 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 누구나 접근 가능
                        .requestMatchers(
                                "/",
                                "/error",
                                "/health",
                                "/health",
                                "/api/auth/**",
                                "api/portfolio/**", "/api/stock/**",
                                "index.html", "/assets/**", "/favicon.ico"
                        ).permitAll()
                        // ADMIN 역할을 가진 사용자만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        // 그외의 URL 요청은 인증되어야 사용 가능
                        .anyRequest().authenticated()
                )
        // API 요청이 securityFilterChain을 지나기 전에 동작
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000")); // 프론트엔드 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
