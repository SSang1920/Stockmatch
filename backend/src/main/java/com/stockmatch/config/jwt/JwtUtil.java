package com.stockmatch.config.jwt;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey cachedSigningKey;

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecretKey();

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        this.cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT SecretKey 초기화 완료");
    }

    private void ensureKeyIsInitialized() {
        if (this.cachedSigningKey == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // AccessToken 생성
    public String generateAccessToken(String userPk, String role){
        ensureKeyIsInitialized();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationTime());

        return Jwts.builder()
                .subject(userPk)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(cachedSigningKey)
                .compact();
    }

    // RefreshToken 생성
    public String generateRefreshToken(String userPk) {
        ensureKeyIsInitialized();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpirationTime());

        return Jwts.builder()
                .subject(userPk)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(cachedSigningKey)
                .compact();
    }

    /**
     * 토큰의 유효성을 검증하고 예외가 발생하면 BusinessException
     * @param token 검증할 토큰
     */
    public void validateTokenOrThrow(String token) {
        ensureKeyIsInitialized();
        try {

            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(cachedSigningKey)
                    .build()
                    .parseSignedClaims(token);


        } catch (ExpiredJwtException e){
            throw e;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("서명 위조 감지!!: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        } catch (Exception e) {
            log.error("검증 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 페이로드(Claims)를 추출
     * @param token 클레임을 추출할 토큰
     * @return 토큰의 페이로드
     */
    private Claims getClaimsFromToken(String token) {
        ensureKeyIsInitialized();
        return Jwts.parser()
                .verifyWith(cachedSigningKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * @param token 사용자 PK를 추출할 토큰
     * @return 사용자 PK 문자열
     */
    public String getUserPkFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

}
