package com.stockmatch.config.jwt;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private SecretKey cachedSigningKey;

    @PostConstruct
    public void init() {
        // JwtProperties에서 secretKey를 가져와 객체 생성후 캐싱
        this.cachedSigningKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    // AccessToken 생성
    public String generateAccessToken(String userPk, String role){
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
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpirationTime());

        return Jwts.builder()
                .subject(userPk)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(cachedSigningKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(cachedSigningKey) //검증에 사용할 비밀 키 설정
                    .build()
                    .parseSignedClaims(token) // 검증
                    .getPayload(); //내용물 추출 & 반환

        } catch (ExpiredJwtException e) {
            //토큰 만료시 에러 처리
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);

        } catch (JwtException | IllegalArgumentException e) {
            // 서명이 다르거나 형식이 잘못된 경우 에러 처리
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public String getUserPkFromToken (String token) {
        return parseClaims(token).getSubject();
    }

    public void validateTokenOrThrow(String token){
        parseClaims(token);
    }

}
