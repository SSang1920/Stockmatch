package com.stockmatch.user.auth.service.login;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.auth.dto.OAuthAttributes;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KakaoLoginHandler extends OAuthLoginHandler {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    //API 엔드포인트 (토큰, 사용자 정보 발급 주소)
    private static final String KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    public KakaoLoginHandler(UserRepository userRepository, JwtUtil jwtUtil, RestTemplate restTemplate) {
        super(userRepository, jwtUtil);
        this.restTemplate = restTemplate;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public Map<String, String> login(String code, String state) {
        // 카카오 서버와 통신하여 토큰 교환
        Map<String, String> socialTokens = exchangeTokens(code);
        String providerAccessToken = socialTokens.get("access_token");
        String providerRefreshToken = socialTokens.get("refresh_token");

        // 사용자 정보 조회
        Map<String, Object> rawProfile = fetchUserProfile(providerAccessToken);
        OAuthAttributes attributes = mapToOAuthAttributes(rawProfile);

        User user = saveOrUpdate(attributes);
        return createAndSaveTokens(user, providerRefreshToken);
    }

    private Map<String, String> exchangeTokens(String code) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoClientId);
        form.add("client_secret", kakaoClientSecret);
        form.add("redirect_uri", kakaoRedirectUri);
        form.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        Map<String, Object> response = restTemplate.postForObject(KAKAO_TOKEN_URI, request, Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }

        Map<String, String> socialTokens = new java.util.HashMap<>();
        socialTokens.put("access_token", (String) response.get("access_token"));
        socialTokens.put("refresh_token", (String) response.get("refresh_token"));

        return socialTokens;
    }

    private Map<String, Object> fetchUserProfile( String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);  // Content-Type: Post 설정
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        return restTemplate.postForObject(KAKAO_USER_INFO_URI, request, Map.class);
    }

    /**
     * provider별 프로필 JSON -> 표준 OAuthAttributes 매핑
     */
    private OAuthAttributes mapToOAuthAttributes(Map<String, Object> profile) {
        return OAuthAttributes.of("kakao", "id", profile);
    }
}
