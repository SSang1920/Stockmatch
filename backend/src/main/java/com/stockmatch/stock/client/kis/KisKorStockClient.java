package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.cache.SecurityNameCacheService;
import com.stockmatch.stock.client.ExternalPriceClient;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;

@Slf4j
@Component
public class KisKorStockClient extends AbstractKisClient implements ExternalPriceClient {

    private final SecurityNameCacheService nameCache;

    private static final Duration NAME_TTL = Duration.ofDays(7);

    public KisKorStockClient(RestTemplate restTemplate,
                             KisTokenProvider kisTokenProvider,
                             SecurityNameCacheService nameCache) {
        super(restTemplate, kisTokenProvider);
        this.nameCache = nameCache;
    }

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
     * 국내 지수 현재가 조회
     */
    public StockPriceResponse getKrIndexPrice(String ticker) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-index-price")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "U")
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.KR_INDEX);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode output = response.getBody() != null ? response.getBody().get("output") : null;

            if (output == null) {
                log.warn("KIS KR Index response empty for {}", ticker);
                return StockPriceResponse.builder().build();
            }

            // API 필드 매핑
            BigDecimal current = parseBigDecimal(output.path("bstp_nmix_prpr").asText());               // 현재지수
            BigDecimal changeAmount = parseBigDecimal(output.path("bstp_nmix_prdy_vrss").asText());     // 전일대비
            BigDecimal changeRate = parseBigDecimal(output.path("bstp_nmix_prdy_ctrt").asText());       // 등락률

            return StockPriceResponse.builder()
                    .region(Region.KR)
                    .ticker(ticker)
                    .name(ticker.equals("0001") ? "KOSPI" : "KOSDAQ")
                    .currentPrice(current)
                    .changeAmount(changeAmount)
                    .changeRate(changeRate)
                    .build();
        } catch (Exception e) {
            log.error("Failed to fetch KR Index {}: {}", ticker, e.getMessage());
            return StockPriceResponse.builder().build();
        }
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

            HttpHeaders headers = createHeaders(KisTrId.KR_REAL_TIME);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

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

        BigDecimal current = new BigDecimal(o.path("stck_prpr").asText("0"));
        BigDecimal prevClose = new BigDecimal(o.path("stck_sdpr").asText("0"));
        BigDecimal open = new BigDecimal(o.path("stck_oprc").asText("0"));
        BigDecimal high = new BigDecimal(o.path("stck_hgpr").asText("0"));
        BigDecimal low = new BigDecimal(o.path("stck_lwpr").asText("0"));
        BigDecimal volume = new BigDecimal(o.path("acml_vol").asText("0"));

        BigDecimal changeAmount = new BigDecimal(o.path("prdy_vrss").asText("0"));
        BigDecimal changePct = new BigDecimal(o.path("prdy_ctrt").asText("0"));
        BigDecimal changeRate = changePct.divide(BigDecimal.valueOf(100));

        return StockPriceResponse.builder()
                .region(Region.KR)
                .ticker(normCode)
                .name(name)
                .currentPrice(current)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .prevClose(prevClose)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .volume(volume)
                .build();
    }
}
