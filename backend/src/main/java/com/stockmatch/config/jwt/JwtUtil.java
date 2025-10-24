package com.stockmatch.config.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    private SecretKey getSigningKey() {
        if (cachedSigningKey == null) {
            byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
            cachedSigningKey = Keys.hmacShaKeyFor(keyBytes);
        }
        return cachedSigningKey;
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
                .signWith(getSigningKey())
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
                .signWith(getSigningKey())
                .compact();
    }

}
