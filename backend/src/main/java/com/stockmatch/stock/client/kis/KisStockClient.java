package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.cache.SecurityNameCacheService;
import com.stockmatch.stock.client.ExternalPriceClient;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Component("kisStockClient")
@RequiredArgsConstructor
public class KisStockClient implements ExternalPriceClient {

    private final RestTemplate restTemplate;
    private final KisTokenProvider kisTokenProvider;
    private final SecurityNameCacheService nameCache;

    private static final Duration NAME_TTL = Duration.ofDays(7);

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.tr-id}")
    private String trId;

    @Override
    public StockPriceResponse getRealtime(String region, String ticker) {
        if (!"KR".equalsIgnoreCase(region)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_REGION);
        }

        return getKoreaPrice(ticker);
    }

    /**
     * 국내 주식 현재가 시세 조회
     */
    public StockPriceResponse getKoreaPrice(String code) {
        String normCode = nameCache.normizeTicker(code);
        JsonNode o = getKoreaPriceRaw(normCode);
        return toStockPrice(normCode, o);
    }

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
     * KIS 원본 JSON 응답 반환
     */
    public JsonNode getKoreaPriceRaw(String code) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")  // 조건 시장 분류 코드
                    .queryParam("FID_INPUT_ISCD", code)
                    .toUriString();

            String accessToken = kisTokenProvider.getAccessToken();
            HttpEntity<Void> entity = new HttpEntity<>(defaultHeaders(accessToken));

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output");
            if (output == null) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            return output;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

    }

    /**
     * Json -> StockPriceResponse 매핑
     */
    private StockPriceResponse toStockPrice(String normCode, JsonNode o) {
        // 캐시/DB
        String name = nameCache.getKrName(normCode);

        // KIS 응답 필드 보조 시도
        if (name == null || name.isBlank()) {
            String kisName = o.path("bstp_kor_isnm").asText(null);
            if (kisName != null && !kisName.isBlank()) {
                nameCache.putKr(normCode, kisName, 24 * 60 * 60L);
                name = kisName;
            }
        }

        if (name == null || name.isBlank()) name = normCode;

        double current = o.path("stck_prpr").asDouble();
        double prevClose = o.path("stck_sdpr").asDouble();
        double open = o.path("stck_oprc").asDouble();
        double high = o.path("stck_hgpr").asDouble();
        double low = o.path("stck_lwpr").asDouble();

        double changePct = o.path("prdy_ctrt").asDouble();
        double changeRate = changePct / 100.0;

        return StockPriceResponse.builder()
                .region(Region.KR)
                .ticker(normCode)
                .name(name)
                .currentPrice(current)
                .prevClose(prevClose)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .changeRate(changeRate)
                .build();
    }
}
