package com.stockmatch.stock.infra.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.stock.dto.Region;
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
     * 공통 요청 헤더
     */
    private HttpHeaders defaultHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);
        headers.set("custtype", "P");

        return headers;
    }

    /**
     * 시세 조회 URL
     */
    private String priceUrl(String code) {
        return UriComponentsBuilder
                .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")  // 조건 시장 분류 코드
                .queryParam("FID_INPUT_ISCD", code)
                .toUriString();
    }

    /**
     * 원본 JSON 응답 반환
     */
    public JsonNode getKoreaPriceRaw(String code) {
        String url = priceUrl(code);
        String accessToken = kisTokenProvider.getAccessToken();
        HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders(accessToken));

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode body = response.getBody();
        if (body == null || body.get("output") == null) {
            throw new IllegalStateException("KIS API 응답이 없습니다.");
        }

        return body.get("output");
    }

    /**
     * Json -> StockPriceResponse 매핑
     */
    private StockPriceResponse toStockPrice(String code, JsonNode o) {
        String name = o.path("bstp_kor_isnm").asText();
        double current = o.path("stck_prpr").asDouble();
        double prevClose = o.path("stck_prdy_clpr").asDouble();
        double open = o.path("stck_oprc").asDouble();
        double high = o.path("stck_hgpr").asDouble();
        double low = o.path("stck_lwpr").asDouble();
        double changeRate = o.path("prdy_ctrt").asDouble();

        return StockPriceResponse.builder()
                .region(Region.KR)
                .ticker(code)
                .name(name)
                .currentPrice(current)
                .prevClose(prevClose)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .changeRate(changeRate)
                .build();
    }

    /**
     * 국내 주식 현재가 시세 조회
     */
    public StockPriceResponse getKoreaPrice(String code) {
        JsonNode o = getKoreaPriceRaw(code);
        return toStockPrice(code, o);
    }

}
