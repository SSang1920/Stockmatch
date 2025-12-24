package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalPriceClient;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisUsStockClient implements ExternalPriceClient {

    private final RestTemplate restTemplate;
    private final KisTokenProvider kisTokenProvider;
    private final SecurityRepository securityRepository;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.tr-id.us.real-time}")
    private String trId;

    @Value("${kis.tr-id.us.index}")
    private String trIdIndex;

    @Override
    public StockPriceResponse getRealtime(String region, String ticker) {
        if (!"US".equalsIgnoreCase(region)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_REGION);
        }

        return getUsPrice(ticker);
    }

    /**
     * 미국 주식 현재가 조회
     */
    public StockPriceResponse getUsPrice(String ticker) {
        String normTicker = normalizeTicker(ticker);

        // 티커 + 미국 마켓으로 종목 조회해서 거래소 확인
        Security security = securityRepository.findByTickerAndMarket(normTicker, Market.US)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        String excd = resolveExcd(security.getExchange());

        JsonNode o = getUsPriceRaw(ticker, excd);
        return toUsStockPrice(security, o);
    }

    /**
     * 해외 지수 시세 조회 및 환율 시세 조회
     */
    public StockPriceResponse getUsIndexPrice(String ticker, String name) {
        try {
            // 티커가 FX로 시작하면 환율(X), 아니면 해외지수(N)
            String marketDivCode = ticker.startsWith("FX") ? "X" : "N";

            // 조회 기간: 오늘 ~ 7일 전
            LocalDate today = LocalDate.now();
            String date1 = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String date2 = today.minusDays(7).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", marketDivCode)
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_DATE_1", date2)
                    .queryParam("FID_INPUT_DATE_2", date1)
                    .queryParam("FID_PERIOD_DIV_CODE", "D")
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + kisTokenProvider.getAccessToken());
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", trIdIndex);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            JsonNode output1 = (body != null) ? body.get("output1") : null;

            if (output1 == null || output1.isEmpty()) {
                log.warn("KIS US Index response empty for {}", ticker);
                return StockPriceResponse.builder().build();
            }

            // API 필드 매핑
            BigDecimal current = parseBigDecimal(output1.path("ovrs_nmix_prpr").asText());
            BigDecimal prevClose = parseBigDecimal(output1.path("ovrs_nmix_prdy_clpr").asText());
            BigDecimal changeAmount = current.subtract(prevClose);
            BigDecimal changeRate = parseBigDecimal(output1.path("prdy_ctrt").asText());

            return StockPriceResponse.builder()
                    .region(Region.US)
                    .ticker(ticker)
                    .name(name)
                    .currentPrice(current)
                    .changeAmount(changeAmount)
                    .changeRate(changeRate)
                    .prevClose(prevClose)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch US Index {}: {}", ticker, e.getMessage());
            return StockPriceResponse.builder().build();
        }
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null) return null;
        return ticker.trim().toUpperCase();
    }

    /**
     * Security.exchange -> KIS EXCD 코드 매핑
     */
    private String resolveExcd(Exchange exchange) {
        if (exchange == null) {
            // 기본값: 나스닥
            return "NAS";
        }

        return switch (exchange) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            default -> "NAS";
        };
    }

    /**
     * KIS 해외 가격 상세 API 원본 JSON 호출
     */
    private JsonNode getUsPriceRaw(String ticker, String excd) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/price-detail")
                    .queryParam("AUTH", "F")
                    .queryParam("EXCD", excd)
                    .queryParam("SYMB", ticker)
                    .toUriString();

            String accessToken = kisTokenProvider.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", trId);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

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

    private StockPriceResponse toUsStockPrice(Security security, JsonNode o) {
        String ticker = security.getTicker();
        String dbName = security.getName();

        BigDecimal current = new BigDecimal(o.path("last").asText("0"));
        BigDecimal prevClose = new BigDecimal(o.path("base").asText("0"));
        BigDecimal open = new BigDecimal(o.path("open").asText("0"));
        BigDecimal high = new BigDecimal(o.path("high").asText("0"));
        BigDecimal low = new BigDecimal(o.path("low").asText("0"));

        // 현재가가 0이면 전일 종가로 대체
        if (current.compareTo(BigDecimal.ZERO) == 0 && prevClose.compareTo(BigDecimal.ZERO) > 0) {
            current = prevClose;
        }

        BigDecimal changeAmount = new BigDecimal(o.path("p_xdif").asText("0"));
        BigDecimal changeRate = new BigDecimal(o.path("p_xrat").asText("0"));

        String name = (dbName != null && !dbName.isBlank()) ? dbName : ticker;

        return StockPriceResponse.builder()
                .region(Region.US)
                .ticker(ticker)
                .name(name)
                .currentPrice(current)
                .changeAmount(changeAmount)
                .changeRate(changeRate)
                .prevClose(prevClose)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .build();
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) return BigDecimal.ZERO;
        try { return new BigDecimal(value.trim()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }
}
