package com.stockmatch.user.auth.service.login;


import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.auth.dto.OAuthAttributes;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public abstract class OAuthLoginHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public abstract AuthProvider getProvider();

    public abstract  Map<String, String> login(String code, @Nullable String state);


    @Transactional
    public Map<String, String> processLoginResponse(OAuthAttributes attributes, String providerRefreshToken){
        User user = userRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(e-> e.updateOAuthInfo(attributes.getName(), attributes.getProfileImageUrl()))
                .orElseGet(attributes::toEntity);

        // 사용자 정보 DB에 Upsert
        user = userRepository.save(user);

        // 우리 서비스의 Jwt 생성, Provider Refresh Token db에 저장
        String ourAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        String ourRefreshToken = jwtUtil.generateRefreshToken(String.valueOf(user.getId()));

        user.updateOurRefreshToken(ourRefreshToken);
        user.updateProviderRefreshToken(providerRefreshToken);

        userRepository.saveAndFlush(user);

        return Map.of("accessToken", ourAccessToken, "refreshToken", ourRefreshToken);
    }
}
