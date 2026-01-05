package com.stockmatch.user.auth.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.config.jwt.JwtUtil;
import com.stockmatch.user.auth.domain.AuthProvider;
import com.stockmatch.user.auth.dto.TokenResponseDto;
import com.stockmatch.user.auth.service.login.OAuthLoginHandler;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final Map<AuthProvider, OAuthLoginHandler> handlerMap;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, List<OAuthLoginHandler> handlers){
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.handlerMap = handlers.stream().collect(
                Collectors.toMap(OAuthLoginHandler::getProvider, Function.identity())
        );
    }

    // 요청을 받으면 해당 Provider LoginHandler로 전달
    public Map<String, String> login(String provider, String code, String state) {
        AuthProvider authProvider = AuthProvider.valueOf(provider.toUpperCase());
        OAuthLoginHandler handler = handlerMap.get(authProvider);

        if (handler == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        return handler.login(code, state);
    }

    /**
     * Refresh Token 을 사용하여 새로운 Access Token 발급
     */
    public TokenResponseDto refreshAccessToken(String refreshTokenHeader) {
        String refreshToken = resolveToken(refreshTokenHeader);

        //토큰 유효성 검사
        jwtUtil.validateTokenOrThrow(refreshToken);

        //사용자 ID추출
        String userPk = jwtUtil.getUserPkFromToken(refreshToken);

        //DB에서 사용자 조회
        User user = userRepository.findById(Long.parseLong(userPk))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //DB에 저장된 refresh Token과 클라이언트가 보낸 토큰의 일치여부 비교
        String savedRefreshToken = user.getRefreshToken();
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            log.warn("Refresh Token 불일치. userPk: {}", userPk);
            // DB에 토큰이 없거나, 보낸 토큰과 다르면 에러 발생
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        //새로운 AccessToken 발급
        String newAccessToken = jwtUtil.generateAccessToken(String.valueOf(user.getId()), user.getRoleKey());
        log.info("Access Token 재발급 성공. userPk: {}", userPk);

        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
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


    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            user.updateRefreshToken(null);
        log.info("로그아웃 처리 완료. userPk: {}", userId);
    }
}
