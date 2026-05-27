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

import java.util.List;

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
                        // 정적/기본 엔드포인트
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api/health",
                                "/index.html", "/assets/**", "/favicon.ico"
                        ).permitAll()

                        // 인증 없이 가능한 API
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**" , "/api/stocks/**", "/api/corporate/**", "/api/market/**", "/api/exchange-rate/**")
                        .permitAll()
                        .requestMatchers("/api/admin/dart/sync").permitAll()

                        // 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 로그인 사용자 전용
                        .requestMatchers("/api/user/**", "/api/portfolio/**", "/api/watchlists/**")
                        .hasAnyRole("USER", "ADMIN")

                        // 그 그외는 인증 필요
                        .anyRequest().authenticated()
                )
        // API 요청이 securityFilterChain을 지나기 전에 동작
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://140.245.67.224", "http://stockmatch.kro.kr")); // Vite React 기본 포트
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
