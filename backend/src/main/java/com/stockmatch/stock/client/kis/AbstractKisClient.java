package com.stockmatch.stock.client.kis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
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

        String upperTrId = (trId != null) ? trId.toUpperCase() : "";

        if (!upperTrId.isEmpty() && (
                upperTrId.startsWith("F") ||
                        upperTrId.startsWith("H") ||
                        upperTrId.startsWith("D") ||
                        upperTrId.startsWith("V") ||
                        upperTrId.contains("NASDAQ") ||
                        upperTrId.contains("S&P") ||
                        upperTrId.contains("USD")
        )) {
            headers.set("authorization", accessToken);
            log.info("[KIS HEADER] Applied pure token format for tr_id: {}", trId);
        } else {
            headers.set("authorization", "Bearer " + accessToken);
            log.info("[KIS HEADER] Applied Bearer token format for tr_id: {}", trId);
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
