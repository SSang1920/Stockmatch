package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalMinutePriceClient;
import com.stockmatch.stock.client.kis.dto.MinutePriceItem;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class KisMinutePriceClient extends AbstractKisClient implements ExternalMinutePriceClient {

    private final SecurityRepository securityRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    public KisMinutePriceClient(RestTemplate restTemplate,
                                KisTokenProvider kisTokenProvider,
                                SecurityRepository securityRepository) {
        super(restTemplate, kisTokenProvider);
        this.securityRepository = securityRepository;
    }

    @Override
    public List<MinutePriceItem> getMinutePrices(String ticker) {
        // DB에서 종목 찾아서 국내/해외 거래소 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        if (security.isKorean()) {
            return getKorMinutePrices(normalizeKrTicker(security.getTicker()));
        } else {
            return getUsMinutePrices(security);
        }
    }

    /**
     * 국내 주식 분봉 조회
     */
    private List<MinutePriceItem> getKorMinutePrices(String ticker) {
        try {
            String dateStr = LocalDate.now().format(DATE_FMT);
            String timeStr = LocalTime.now().format(TIME_FMT);

            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_HOUR_1", timeStr)
                    .queryParam("FID_INPUT_DATE_1", dateStr)
                    .queryParam("FID_PW_DATA_INCU_YN", "Y")
                    .queryParam("FID_FAKE_TICK_INCU_YN", "")
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.KR_MINUTE_PRICE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output2");

            if (output == null || !output.isArray()) {
                return List.of();
            }

            List<MinutePriceItem> result = new ArrayList<>();

            for (JsonNode node : output) {
                // 날짜, 시간 파싱
                String dateRaw = node.path("stck_bsop_date").asText();
                String timeRaw = node.path("stck_cntg_hour").asText();

                LocalDate date = LocalDate.parse(dateRaw, DATE_FMT);
                LocalTime time = LocalTime.parse(timeRaw, TIME_FMT);
                LocalDateTime dateTime = LocalDateTime.of(date, time);

                // 가격 정보 파싱
                BigDecimal close = new BigDecimal(node.path("stck_prpr").asText("0"));
                BigDecimal open = new BigDecimal(node.path("stck_oprc").asText("0"));
                BigDecimal high = new BigDecimal(node.path("stck_hgpr").asText("0"));
                BigDecimal low = new BigDecimal(node.path("stck_lwpr").asText("0"));
                BigDecimal volume = new BigDecimal(node.path("cntg_vol").asText("0"));

                result.add(new MinutePriceItem(dateTime, open, high, low, close, volume));
            }

            // 시간 오름차순 정렬
            result.sort(Comparator.comparing(MinutePriceItem::dateTime));

            return result;
        } catch (Exception e) {
            log.error("[KIS-KR] error ticker={}", ticker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

    }

    /**
     * 해외 주식 분봉 조회
     */
    private List<MinutePriceItem> getUsMinutePrices(Security security) {
        String ticker = security.getTicker();
        String excd = resolveExcd(security.getExchange());

        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice")
                    .queryParam("AUTH", "")
                    .queryParam("EXCD", excd)
                    .queryParam("SYMB", ticker)
                    .queryParam("NMIN", "1")
                    .queryParam("PINC", "1")
                    .queryParam("NEXT", "")
                    .queryParam("NREC", "120")
                    .queryParam("FILL", "")
                    .queryParam("KEYB", "")
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.US_MINUTE_PRICE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output2");

            if (output == null || !output.isArray()) {
                return List.of();
            }

            List<MinutePriceItem> result = new ArrayList<>();

            for (JsonNode node : output) {
                String dateRaw = node.path("kymd").asText();
                String timeRaw = node.path("khms").asText();

                LocalDate date = LocalDate.parse(dateRaw, DATE_FMT);
                LocalTime time = LocalTime.parse(timeRaw, TIME_FMT);
                LocalDateTime dateTime = LocalDateTime.of(date, time);

                BigDecimal open = new BigDecimal(node.path("open").asText("0"));
                BigDecimal high = new BigDecimal(node.path("high").asText("0"));
                BigDecimal low = new BigDecimal(node.path("low").asText("0"));
                BigDecimal close = new BigDecimal(node.path("last").asText("0"));
                BigDecimal volume = new BigDecimal(node.path("evol").asText("0"));

                result.add(new MinutePriceItem(dateTime, open, high, low, close, volume));
            }

            // 시간 오름차순 정렬
            result.sort(Comparator.comparing(MinutePriceItem::dateTime));

            return result;

        } catch (Exception e) {
            log.error("[KIS-US] daily-minute chart error ticker={}", ticker, e);
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
     * DB Exchange -> KIS EXCD 코드 매핑
     */
    private String resolveExcd(Exchange exchange) {
        if (exchange == null) return "NAS"; // 기본값

        return switch (exchange) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            case AMEX -> "AMS";
            default -> "NAS";
        };
    }
}
