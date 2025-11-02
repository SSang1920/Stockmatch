package com.stockmatch.user.auth.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.auth.dto.OAuthAttributes;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;

    // 카카오
    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;
    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    //네이버
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    //구글
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    //API 엔드포인트 (토큰, 사용자 정보 발급 주소)
    private static final String KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";
    private static final String NAVER_TOKEN_URI = "https://nid.naver.com/oauth2.0/token";
    private static final String NAVER_USER_INFO_URI = "https://openapi.naver.com/v1/nid/me";
    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URI = "https://www.googleapis.com/oauth2/v2/userinfo";

    public Map<String, String> kakaoLogin(String code) {
        return doLogin("kakao", code, null);
    }

    public Map<String, String> naverLogin(String code, String state) {
        return doLogin("naver", code, state);
    }

    public Map<String, String> googleLogin(String code) {
        return doLogin("google", code, null);
    }

    /**
     * 공통 로그인
     */
    private Map<String, String> doLogin(String provider, String code, String state) {
        // Provider의 API를 호출할 때 사용할 토큰 발급
        Map<String, String> socialTokens = exchangeTokens(provider, code, state);
        String providerAccessToken = socialTokens.get("access_token"); // "네이버/구글용 Access Token"
        String providerRefreshToken = socialTokens.get("refresh_token"); // "네이버/구글용 Refresh Token"

        // 엑세스 토큰으로 사용자 프로필 조회
        Map<String, Object> rawProfile = fetchUserProfile(provider, providerAccessToken);

        // provider별 프로필 구조를 표준 DTO로 매핑
        OAuthAttributes attributes = mapToOAuthAttributes(provider, rawProfile);

        // 사용자 upsert(DB 저장/수정) + JWT(access/refresh) 발급
        User user = saveOrUpdate(attributes);

        //우리 서버의 accessToken, refreshToken 발급
        return createAndSaveTokens(user, providerRefreshToken);
    }

    /**
     * 인가코드 -> 엑세스 토큰 교환
     */
    private Map<String,String> exchangeTokens(String provider, String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        String tokenUri;
        switch (provider) {
            case "kakao" -> {
                tokenUri = KAKAO_TOKEN_URI;
                form.add("client_id", kakaoClientId);
                form.add("client_secret", kakaoClientSecret);
                form.add("redirect_uri", kakaoRedirectUri);
                form.add("code", code);
            }

            case "naver" -> {
                tokenUri = NAVER_TOKEN_URI;
                form.add("client_id", naverClientId);
                form.add("client_secret", naverClientSecret);
                form.add("redirect_uri", naverRedirectUri);
                form.add("code", code);
                if (StringUtils.hasText(state)) form.add("state", state);
            }

            case "google" -> {
                tokenUri = GOOGLE_TOKEN_URI;
                form.add("client_id", googleClientId);
                form.add("client_secret", googleClientSecret);
                form.add("redirect_uri", googleRedirectUri);
                form.add("code", code);
            }

            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
        Map<String, Object> response = restTemplate.postForObject(tokenUri, request, Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILED);
        }

        Map<String, String> socialTokens = new java.util.HashMap<>();
        socialTokens.put("access_token", (String) response.get("access_token"));
        socialTokens.put("refresh_token", (String) response.get("refresh_token"));

        return socialTokens;
    }

    /**
     * 엑세스 토큰으로 사용자 프로필 조회
     */
    private Map<String, Object> fetchUserProfile(String provider, String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        return switch (provider) {
            case "kakao" -> {
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);  // Content-Type: Post 설정
                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
                yield restTemplate.postForObject(KAKAO_USER_INFO_URI, request, Map.class);
            }

            case "naver" -> {
                HttpEntity<Void> request = new HttpEntity<>(headers);
                yield restTemplate.postForObject(NAVER_USER_INFO_URI, request, Map.class);
            }

            case "google" -> {
                HttpEntity<Void> request = new HttpEntity<>(headers);
                yield restTemplate.getForObject(GOOGLE_USER_INFO_URI + "?access_token=" + accessToken, Map.class);
            }

            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    /**
     * provider별 프로필 JSON -> 표준 OAuthAttributes 매핑
     */
    @SuppressWarnings("unchecked")
    private OAuthAttributes mapToOAuthAttributes(String provider, Map<String, Object> profile) {
        return switch (provider) {
            case "kakao" -> OAuthAttributes.of("kakao", "id", profile);

            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) profile.get("response");
                if (response == null) throw new BusinessException(ErrorCode.OAUTH_USERINFO_FAILED);
                yield OAuthAttributes.of("naver", "id", Map.of("response", response));
            }

            case "google" -> OAuthAttributes.of("google", "sub", profile);

            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }

    /**
     * 사용자 upsert(DB 저장/수정) 로직
     */
    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByProviderAndProviderId(
                        attributes.getProvider(), attributes.getProviderId())
                .map(e -> e.updateOAuthInfo(attributes.getName(), attributes.getProfileImageUrl()))
                .orElse(attributes.toEntity());
        return userRepository.save(user);
    }

    /**
     * Refresh Token 을 사용하여 새로운 Access Token 발급
     */
    public Map<String, String> refreshAccessToken(String refreshTokenHeader) {
        String refreshToken = resolveToken(refreshTokenHeader);
        jwtUtil.validateTokenOrThrow(refreshToken);

        String userPk = jwtUtil.getUserPkFromToken(refreshToken);
        User user = userRepository.findById(Long.parseLong(userPk))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        log.info("Access Token 재발급 성공. userPk: {}", userPk);

        return Map.of("accessToken", newAccessToken);
    }

    /**
     * 실제 토큰 추출
     */
    private String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

    /**
     * Access/Refresh 토큰 생성 및 저장
     */
    private Map<String, String> createAndSaveTokens(User user, String providerRefreshToken) {
        String ourAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        String ourRefreshToken = jwtUtil.generateRefreshToken(String.valueOf(user.getId()));

        user.updateRefreshToken(providerRefreshToken); // DB에 Refresh Token 저장

        return Map.of("accessToken", ourAccessToken, "refreshToken", ourRefreshToken);
    }

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            user.updateRefreshToken(null);
        log.info("로그아웃 처리 완료. userPk: {}", userId);
    }
}
