package com.stockmatch.stock.client.kis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@RequiredArgsConstructor
public abstract class AbstractKisClient {

    protected final RestTemplate restTemplate;
    protected final KisTokenProvider kisTokenProvider;

    @Value("${kis.base-url}")
    protected String baseUrl;

    @Value("${kis.app-key}")
    protected String appKey;

    @Value("${kis.app-secret}")
    protected String appSecret;

    /**
     * 공통 헤더 생성
     */
    protected HttpHeaders createHeaders(String trId) {
        String accessToken = kisTokenProvider.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (trId != null && (trId.startsWith("F") || trId.startsWith("H") || trId.startsWith("D") || trId.startsWith("V"))) {
            headers.set("authorization", accessToken);
        } else {
            headers.set("authorization", "Bearer " + accessToken);
        }

        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", "P");

        return headers;
    }

    /**
     * 숫자 파싱 유틸
     */
    protected BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
