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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class NaverLoginHandler extends OAuthLoginHandler {

    private final RestTemplate restTemplate;

    //네이버
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    //API 엔드포인트 (토큰, 사용자 정보 발급 주소)
    private static final String NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";

    public NaverLoginHandler(UserRepository userRepository, JwtUtil jwtUtil, RestTemplate restTemplate) {
        super(userRepository, jwtUtil);
        this.restTemplate = restTemplate;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.NAVER;
    }

    @Override
    public Map<String, String> login(String code, String state) {
        Map<String, String> socialTokens = exchangeTokens(code, state);
        String providerAccessToken = socialTokens.get("access_token");
        String providerRefreshToken = socialTokens.get("refresh_token");

        Map<String, Object> rawProfile = fetchUserProfile(providerAccessToken);
        OAuthAttributes attributes = mapToOAuthAttributes(rawProfile);

        return processLoginResponse(attributes, providerRefreshToken);
    }

    private Map<String, String> exchangeTokens(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", naverClientId);
        form.add("client_secret", naverClientSecret);
        form.add("redirect_uri", naverRedirectUri);
        form.add("code", code);

        if (StringUtils.hasText(state)) form.add("state", state);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        Map<String, Object> response = restTemplate.postForObject(NAVER_TOKEN_URI, request, Map.class);

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

        return restTemplate.postForObject(NAVER_USER_INFO_URI, request, Map.class);
    }

    /**
     * provider별 프로필 JSON -> 표준 OAuthAttributes 매핑
     */
    @SuppressWarnings("unchecked")
    private OAuthAttributes mapToOAuthAttributes(Map<String, Object> profile) {
        Map<String, Object> response = (Map<String, Object>) profile.get("response");
        if (response == null) throw new BusinessException(ErrorCode.OAUTH_USERINFO_FAILED);

        return OAuthAttributes.of("naver", "id", Map.of("response", response));
    }
}
