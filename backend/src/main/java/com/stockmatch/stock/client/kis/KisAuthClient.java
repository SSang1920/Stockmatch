package com.stockmatch.stock.client.kis;

import com.stockmatch.stock.client.kis.dto.KisAuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KisAuthClient {

    private final RestTemplate restTemplate;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    public KisAuthResponse requestAccessToken() {
        String url = baseUrl + "/oauth2/tokenP";

        Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );

        return restTemplate.postForObject(url, body, KisAuthResponse.class);
    }
}
