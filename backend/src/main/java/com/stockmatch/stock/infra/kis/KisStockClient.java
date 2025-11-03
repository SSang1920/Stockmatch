package com.stockmatch.stock.infra.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.stock.dto.StockPriceResponse;
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
     */
    public StockPriceResponse getKoreaPrice(String code) {
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
        JsonNode body = response.getBody();

        if (body == null || body.get("output") == null) {
            throw new IllegalStateException("KIS API 응답이 없습니다.");
        }

        JsonNode o = body.get("output");

        // KIS 응답 JSON -> StockPriceResponse 변환
        return StockPriceResponse.builder()
                .symbol(code)
                .name(o.path("bstp_kor_isnm").asText())         // 종목명
                .currentPrice(o.path("stck_prpr").asDouble())   // 현재가
                .prevClose(o.path("prdy_vrss").asDouble())      // 전일 종가
                .openPrice(o.path("stck_oprc").asDouble())      // 시가
                .highPrice(o.path("stck_hgpr").asDouble())      // 고가
                .lowPrice(o.path("stck_lwpr").asDouble())       // 저가
                .changeRate(o.path("prdy_ctrt").asDouble())     // 등락률
                .build();
    }

}
