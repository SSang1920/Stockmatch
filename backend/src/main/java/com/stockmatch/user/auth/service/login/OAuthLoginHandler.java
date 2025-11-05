package com.stockmatch.user.auth.service.login;


import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.auth.dto.OAuthAttributes;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;


@RequiredArgsConstructor
public abstract class OAuthLoginHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public abstract AuthProvider getProvider();

    public abstract  Map<String, String> login(String code, @Nullable String state);

    /**
     * 사용자 정보 DB에 Upsert
     */
    @Transactional
    protected User saveOrUpdate(OAuthAttributes attributes) {
        User user = userRepository.findByProviderAndProviderId(attributes.getProvider(), attributes.getProviderId())
                .map(e-> e.updateOAuthInfo(attributes.getName(), attributes.getProfileImageUrl()))
                .orElse(attributes.toEntity());

        return userRepository.save(user);
    }

    /**
     * 우리 서비스의 Jwt 생성, Provider Refresh Token db에 저장
     */
    @Transactional
    protected Map<String,String> createAndSaveTokens(User user, String providerRefreshToken){
        String ourAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        String ourRefreshToken = jwtUtil.generateRefreshToken(String.valueOf(user.getId()));

        user.updateRefreshToken(providerRefreshToken);

        return Map.of("accessToken", ourAccessToken, "refreshToken", ourRefreshToken);
    }
}
