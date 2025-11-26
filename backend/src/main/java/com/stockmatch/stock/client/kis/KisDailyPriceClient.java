package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalDailyPriceClient;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Security;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisDailyPriceClient implements ExternalDailyPriceClient {

    private static final DateTimeFormatter KIS_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KisTokenProvider kisTokenProvider;
    private final SecurityRepository securityRepository;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.tr-id.kr.daily-price}")
    private String krDailyTrId;

    @Value("${kis.tr-id.us.daily-price}")
    private String usDailyTrId;

    @Override
    public List<DailyPriceItem> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // DB에서 종목 찾아서 국내/해외 거래소 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        if (security.isKorean()) {
            return getKorDailyPrices(normalizeKrTicker(security.getTicker()), from, to);
        } else {
            return getUsDailyPrices(security, from, to);
        }
    }

    /**
     * 국내 일별 시세 조회
     */
    private List<DailyPriceItem> getKorDailyPrices(String krTicker, LocalDate from, LocalDate to) {

        try {
            // URL
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")   // 조건 시장 분류 코드(J:KRX, NX:NXT, UN:통합)
                    .queryParam("FID_INPUT_ISCD", krTicker)               // 종목코드
                    .queryParam("FID_INPUT_DATE_1", from)               // 조회 시작일자
                    .queryParam("FID_INPUT_DATE_2", to)                 // 조회 종료일자 (최대 100개)
                    .queryParam("FID_PERIOD_DIV_CODE", "D")     // 기간분류코드(D:일봉 W:주봉, M:월봉, Y:년봉)
                    .queryParam("FID_ORG_ADJ_PRC", "0")         // 수정주가 원주가 가격 여부(0:수정주가 1:원주가)
                    .toUriString();

            // 헤더
            String accessToken = kisTokenProvider.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", krDailyTrId);
            headers.set("custtype", "P");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output2");

            if (output == null || !output.isArray()) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            List<DailyPriceItem> result = new ArrayList<>();

            for (JsonNode node : output) {
                String dateStr = node.path("stck_bsop_date").asText(null);
                if (dateStr == null || dateStr.isBlank()) continue;

                LocalDate date = LocalDate.parse(dateStr, KIS_DATE);

                // from/to 범위 필터링
                if (from != null && date.isBefore(from)) continue;
                if (to != null && date.isAfter(to)) continue;

                BigDecimal open = new BigDecimal(node.path("stck_oprc").asText("0"));
                BigDecimal close = new BigDecimal(node.path("stck_clpr").asText("0"));
                BigDecimal high = new BigDecimal(node.path("stck_hgpr").asText("0"));
                BigDecimal low = new BigDecimal(node.path("stck_lwpr").asText("0"));
                BigDecimal volume = new BigDecimal(node.path("acml_vol").asText("0"));

                result.add(new DailyPriceItem(
                        date,
                        open,
                        close,
                        high,
                        low,
                        volume
                ));
            }

            // 날짜 오름차순 정렬
            result.sort(Comparator.comparing(DailyPriceItem::date));

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS-KR] daily price error. ticker={}", krTicker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 티커 정규화: 숫자만 들어올경우 6자리 숫자 변경
     */
    private String normalizeKrTicker(String ticker) {
        if (ticker == null) return null;

        ticker = ticker.trim();
        if (ticker.matches("\\d+")) {
            return String.format("%06d", Integer.parseInt(ticker));
        }
        return ticker;
    }

    /**
     * 해외 일별 시세 조회
     */
    private List<DailyPriceItem> getUsDailyPrices(Security security, LocalDate from, LocalDate to) {

        // 티커, 거래소 추출
        String ticker = security.getTicker();
        String excd = resolveExcd(security.getExchange());

        try {
            String start = (from != null) ? from.format(KIS_DATE) : "";
            String end = (to != null) ? to.format(KIS_DATE) : "";

            // URL
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/dailyprice")
                    .queryParam("AUTH", "")
                    .queryParam("EXCD", excd)
                    .queryParam("SYMB", ticker)
                    .queryParam("GUBN", "0")
                    .queryParam("BYMD", end)
                    .queryParam("MODP", "0")
                    .toUriString();

            // 헤더
            String accessToken = kisTokenProvider.getAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization", "Bearer " + accessToken);
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("tr_id", usDailyTrId);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output2");

            if (output == null || !output.isArray()) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            List<DailyPriceItem> result = new ArrayList<>();

            for (JsonNode node : output) {
                String dateStr = node.path("xymd").asText(null);
                if (dateStr == null || dateStr.isBlank()) continue;

                LocalDate date = LocalDate.parse(dateStr, KIS_DATE);

                // from/to 범위 필터링
                if (from != null && date.isBefore(from)) continue;
                if (to != null && date.isAfter(to)) continue;

                BigDecimal open = new BigDecimal(node.path("open").asText("0"));
                BigDecimal close = new BigDecimal(node.path("clos").asText("0"));
                BigDecimal high = new BigDecimal(node.path("high").asText("0"));
                BigDecimal low = new BigDecimal(node.path("low").asText("0"));
                BigDecimal volume = new BigDecimal(node.path("tvol").asText("0"));

                result.add(new DailyPriceItem(
                        date,
                        open,
                        close,
                        high,
                        low,
                        volume
                ));
            }

            // 날짜 오름차순 정렬
            result.sort(Comparator.comparing(DailyPriceItem::date));

            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS-US] daily price error. ticker={}", ticker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * DB Exchange -> KIS EXCD 코드 매핑
     */
    private String resolveExcd(Exchange exchange) {
        if (exchange == null) return "NAS"; // 기본값

        return switch (exchange) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            default -> "NAS";
        };
    }
}
