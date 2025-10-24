package com.stockmatch.config;

import com.stockmatch.config.handler.OAuth2AuthenticationSuccessHandler;
import com.stockmatch.user.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService CustomOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                //csrf 보호 비활성화 (JWT 사용 시)
                .csrf(csrf -> csrf.disable())

                // 세션 관리 정책 STETELESS 설정 (세션 사용X)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청 인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/", "index.html", "/assets/**", "/favicon.ico", "/api/**").permitAll()
                        .requestMatchers("/login/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        //사용자 정보를 가져오는 엔드 포인트에 대한 설정
                        .userInfoEndpoint(userInfo -> userInfo
                                //OAuth에서 준 양식을 그대로 사용하지 않고 CustomOAuthService로 보내 양식 변하게 함
                                .userService(CustomOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080")); // 프론트엔드 주소
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
