package com.stockmatch.user.auth.service.unlink;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.member.domain.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverUnlinker implements OAuthUnlinker {

    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;
    private static final String NAVER_TOKEN_URL = "https://nid.naver.com/oauth2.0/token";

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.NAVER;
    }

    @Override
    public void unlink(User user){
        String providerRefreshToken = user.getProviderRefreshToken();

        if (providerRefreshToken == null) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        // 새 Access Token 갱신 ( 우리 서버 x, 네이버의 accessToken)
        String newAccessToken = refreshNaverAccessToken(providerRefreshToken);

        // 연동 해제 요청
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "delete");
        formData.add("client_id", naverClientId);
        formData.add("client_secret", naverClientSecret);
        formData.add("access_token", newAccessToken);
        formData.add("service_provider", "NAVER");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, null);
        String response = restTemplate.postForObject(NAVER_TOKEN_URL, request, String.class);
        log.info("Naver unlink response: {}", response);

    }

    /**
     * AccessToken 갱신
     */
    private String refreshNaverAccessToken(String providerRefreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", naverClientId);
        formData.add("client_secret", naverClientSecret);
        formData.add("refresh_token", providerRefreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, null);
        NaverTokenRefreshResponse response = restTemplate.postForObject(NAVER_TOKEN_URL, request, NaverTokenRefreshResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new IllegalStateException("Failed to refresh Naver access token.");
        }
        return response.getAccessToken();
    }

    /**
     * 네이버 토큰 갱신 응답
     */
    @Getter
    private static class NaverTokenRefreshResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private String expiresIn;
    }
}
