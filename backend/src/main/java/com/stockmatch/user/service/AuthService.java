package com.stockmatch.user.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.dto.OAuthAttributes;
import com.stockmatch.user.repository.UserRepository;
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
     * 소셜 로그인
     * @param code = 인가 코드 (Authorization Code)
     * @return
     */
    public Map<String, String> kakaoLogin(String code) {
        String kakaoAccessToken = getKakaoAccessToken(code);
        Map<String, Object> userInfoAttributes = getKakaoUserInfo(kakaoAccessToken);
        OAuthAttributes attributes = OAuthAttributes.of("kakao", "id", userInfoAttributes);
        User user = saveOrUpdate(attributes);

        return createAndSaveTokens(user);
    }

    public Map<String, String> naverLogin(String code, String state) {
        String naverAccessToken = getNaverAccessToken(code, state);
        Map<String, Object> userInfoAttributes = getNaverUserInfo(naverAccessToken);
        OAuthAttributes attributes = OAuthAttributes.of("naver", "id", userInfoAttributes);
        User user = saveOrUpdate(attributes);

        return createAndSaveTokens(user);
    }

    public Map<String, String> googleLogin(String code) {
        String googleAccessToken = getGoogleAccessToken(code);
        Map<String, Object> userInfoAttributes = getGoogleUserInfo(googleAccessToken);
        OAuthAttributes attributes = OAuthAttributes.of("google", "sub", userInfoAttributes);
        User user = saveOrUpdate(attributes);

        return createAndSaveTokens(user);
    }

    private String getKakaoAccessToken(String code){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        Map<String, Object> response = restTemplate.postForObject(KAKAO_TOKEN_URI, request, Map.class);

        return (String) response.get("access_token");
    }

    private Map<String, Object> getKakaoUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        // POST 요청으로 액세스 토큰 요청
        return restTemplate.postForObject(KAKAO_USER_INFO_URI, request, Map.class);
    }

    private String getNaverAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId); //REST API 키
        params.add("client_secret", naverClientSecret); //시크릿키
        params.add("redirect_uri", naverRedirectUri);
        params.add("code", code); //인가코드
        params.add("state", state);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        Map<String, Object> response = restTemplate.postForObject(NAVER_TOKEN_URI, request, Map.class);

        return (String) response.get("access_token");
    }

    private Map<String, Object> getNaverUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        return restTemplate.postForObject(NAVER_USER_INFO_URI, request, Map.class);
    }

    private String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        Map<String, Object> response = restTemplate.postForObject(GOOGLE_TOKEN_URI, request, Map.class);

        return (String) response.get("access_token");
    }

    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);

        return restTemplate.getForObject(GOOGLE_USER_INFO_URI + "?access_token=" + accessToken, Map.class);
    }

    private User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByProviderAndProviderId(
                        attributes.getProvider(),
                        attributes.getProviderId()
                )
                .map(entity -> entity.updateOAuthInfo(attributes.getName(), attributes.getProfileImageUrl()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }

    private String resolveToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

    private Map<String, String> createAndSaveTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        String refreshToken = jwtUtil.generateRefreshToken(String.valueOf(user.getId()));
        user.updateRefreshToken(refreshToken); // DB에 Refresh Token 저장

        return Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }


}
