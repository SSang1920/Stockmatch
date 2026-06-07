package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.cache.SecurityNameCacheService;
import com.stockmatch.stock.client.ExternalPriceClient;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class KisKorStockClient extends AbstractKisClient implements ExternalPriceClient {

    private final SecurityNameCacheService nameCache;
    private final SecurityRepository securityRepository;
    private final KisApiHelper kisApiHelper;

    private final Set<String> invalidTickers = Collections.synchronizedSet(new HashSet<>());
    private static final Duration NAME_TTL = Duration.ofDays(7);

    public KisKorStockClient(RestTemplate restTemplate,
                             KisTokenProvider kisTokenProvider,
                             SecurityNameCacheService nameCache,
                             SecurityRepository securityRepository,
                             KisApiHelper kisApiHelper) {
        super(restTemplate, kisTokenProvider);
        this.nameCache = nameCache;
        this.securityRepository = securityRepository;
        this.kisApiHelper = kisApiHelper;
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

        // 티커 + 한국 마켓 조건으로 디비 우선 조회, 없을 시 자동으로 KIS 마스터 API 구동
        Security security = securityRepository.findByTickerAndMarket(normCode, Market.KR)
                .orElseGet(() -> fetchAndSaveNewSecurity(normCode));

        if (security == null) {
            throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
        }

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

    @Override
    public Security fetchCompanyProfile(String ticker, String region) {
        String normCode = nameCache.normizeTicker(ticker);
        String logPrefix = String.format("[KIS 국내 프로필 수집 - %s]", normCode);

        return kisApiHelper.execute(() -> {

            // 1. 제공해주신 국장 전용 명세 URL 경로 및 파라미터 세팅
            String path = "/uapi/domestic-stock/v1/quotations/search-info";
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                    .queryParam("PRDT_TYPE_CD", "300") // 300: 국내 주식 상품유형코드 고정
                    .queryParam("PDNO", normCode)
                    .build()
                    .toUriString();

            HttpHeaders headers = createHeaders("CTPF1604R");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = responseEntity.getBody();
            if (body == null || !"0".equals(String.valueOf(body.get("rt_cd")))) {
                log.warn("{} KIS 국장 마스터 API 응답 실패", logPrefix);
                return null;
            }

            Map<String, Object> output = (Map<String, Object>) body.get("output");
            if (output == null) {
                return null;
            }

            String korName = output.get("prdt_name") != null ? String.valueOf(output.get("prdt_name")).trim() : "";
            String engName = output.get("prdt_eng_name") != null ? String.valueOf(output.get("prdt_eng_name")).trim() : "";

            if (korName.isEmpty() && output.get("prdt_abrv_name") != null) {
                korName = String.valueOf(output.get("prdt_abrv_name")).trim();
            }

            Exchange targetExchange = Exchange.KOSPI;

            String clsfName = output.get("prdt_clsf_name") != null ? String.valueOf(output.get("prdt_clsf_name")) : "";
            if (clsfName.contains("코스닥") || clsfName.contains("KOSDAQ")) {
                targetExchange = Exchange.KOSDAQ;
            }

            if (korName.isEmpty()) korName = normCode;
            if (engName.isEmpty()) engName = normCode;

            log.info("{} 국장 스크래핑 완공 -> 거래소: {}, 종목명: {}, 영문명: {}",
                    logPrefix, targetExchange, korName, engName);

            return Security.builder()
                    .ticker(normCode)
                    .name(korName)
                    .englishName(engName)
                    .market(Market.KR) // 국내 마켓 고정
                    .exchange(targetExchange)
                    .currency(Currency.KRW)
                    .delisted(false)
                    .build();

        }, logPrefix);
    }

    private Security fetchAndSaveNewSecurity(String ticker) {
        if (invalidTickers.contains(ticker)) {
            return null;
        }

        try {
            Security newSecurity = fetchCompanyProfile(ticker, "KR");
            if (newSecurity != null) {
                Security saved = securityRepository.save(newSecurity);
                log.info("[국장 LAZY-LOADING 완공] 신규 국내 종목 적재 성공: {} ({})", ticker, saved.getName());
                return saved;
            }
        } catch (Exception e) {
            log.error("Lazy loading profile fetch error for domestic ticker: {}", ticker, e);
        }

        invalidTickers.add(ticker);
        return null;
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
