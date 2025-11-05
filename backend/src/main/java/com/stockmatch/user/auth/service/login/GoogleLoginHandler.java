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
public class GoogleLoginHandler extends OAuthLoginHandler {

    private final RestTemplate restTemplate;

    // 구글 전용 설정값
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    //API 엔드포인트 (토큰, 사용자 정보 발급 주소)
    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URI = "https://www.googleapis.com/oauth2/v2/userinfo";

    public GoogleLoginHandler(UserRepository userRepository, JwtUtil jwtUtil, RestTemplate restTemplate) {
        super(userRepository, jwtUtil);
        this.restTemplate = restTemplate;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public Map<String, String> login(String code, String state) {
        Map<String, String> socialTokens = exchangeTokens(code);
        String providerAccessToken = socialTokens.get("access_token");
        String providerRefreshToken = socialTokens.get("refresh_token");

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
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("redirect_uri", googleRedirectUri);
        form.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        Map<String, Object> response = restTemplate.postForObject(GOOGLE_TOKEN_URI, request, Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }

        Map<String, String> socialTokens = new java.util.HashMap<>();
        socialTokens.put("access_token", (String) response.get("access_token"));
        socialTokens.put("refresh_token", (String) response.get("refresh_token"));

        return socialTokens;
    }

    private Map<String, Object> fetchUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        return restTemplate.getForObject(GOOGLE_USER_INFO_URI + "?access_token=" + accessToken, Map.class);
    }

    /**
     * provider별 프로필 JSON -> 표준 OAuthAttributes 매핑
     */
    private OAuthAttributes mapToOAuthAttributes(Map<String, Object> profile) {
        return OAuthAttributes.of("google", "sub", profile);
    }
}
