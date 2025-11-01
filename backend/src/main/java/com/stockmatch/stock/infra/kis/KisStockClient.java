package com.stockmatch.stock.infra.kis;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KisStockClient {

    private final RestTemplate restTemplate;
    private final KisTokenProvider kisTokenProvider;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.tr-id}")
    private String trId;

    /**
     * 국내 주식 현재가 시세 조회
     * @param code KRX 종목코드
     * @return
     */
    public JsonNode getKoreaPrice(String code) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")  // 조건 시장 분류 코드
                .queryParam("FID_INPUT_ISCD", code)
                .toUriString();

        String accessToken = kisTokenProvider.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        return response.getBody();
    }

}
