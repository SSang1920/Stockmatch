package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalDailyPriceClient;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class KisDailyPriceClient extends AbstractKisClient implements ExternalDailyPriceClient {

    private final SecurityRepository securityRepository;

    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int KOR_CHUNK_DAYS = 90;

    public KisDailyPriceClient(RestTemplate restTemplate,
                               KisTokenProvider kisTokenProvider,
                               SecurityRepository securityRepository) {
        super(restTemplate, kisTokenProvider);
        this.securityRepository = securityRepository;
    }

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
     * 국내 일별 시세 조회 (여러 번 호출해서 합)
     */
    private List<DailyPriceItem> getKorDailyPrices(String krTicker, LocalDate from, LocalDate to) {

        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        List<DailyPriceItem> result = new ArrayList<>();

        // 1번 호출당 100회 제한: 1청크당 90일 자르기
        LocalDate cursorFrom = from;
        while (!cursorFrom.isAfter(to)) {
            LocalDate cursorTo = cursorFrom.plusDays(KOR_CHUNK_DAYS - 1);   // 90일 청크
            if (cursorTo.isAfter(to)) {
                cursorTo = to;
            }

            JsonNode output = callKorDailyPriceApi(krTicker, cursorFrom, cursorTo);

            if (output.isArray()) {
                for (JsonNode node : output) {
                    DailyPriceItem item = toKorDailyItem(node);
                    LocalDate d = item.date();

                    // 전체 from/to 범위 다시 한번 필터링
                    if (!d.isBefore(from) && !d.isAfter(to)) {
                        result.add(item);
                    }
                }
            }

            // 다음 청크
            cursorFrom = cursorTo.plusDays(1);
        }

        // 날짜 오름차순 정렬
        result.sort(Comparator.comparing(DailyPriceItem::date));
        return result;
    }

    /**
     * KIS 국내 일별 시세 API 한 번 호출 (최대 100건)
     */
    private JsonNode callKorDailyPriceApi(String krTicker, LocalDate from, LocalDate to) {
        try {

            // URL
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")       // 조건 시장 분류 코드(J:KRX, NX:NXT, UN:통합)
                    .queryParam("FID_INPUT_ISCD", krTicker)                 // 종목코드
                    .queryParam("FID_INPUT_DATE_1", from.format(KIS_DATE))  // 조회 시작일자
                    .queryParam("FID_INPUT_DATE_2", to.format(KIS_DATE))    // 조회 종료일자 (최대 100개)
                    .queryParam("FID_PERIOD_DIV_CODE", "D")          // 기간분류코드(D:일봉 W:주봉, M:월봉, Y:년봉)
                    .queryParam("FID_ORG_ADJ_PRC", "0")              // 수정주가 원주가 가격 여부(0:수정주가 1:원주가)
                    .toUriString();

            // 헤더
            HttpHeaders headers = createHeaders(KisTrId.KR_DAILY_PRICE);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            JsonNode output = (body == null) ? null : body.get("output2");

            if (output == null || !output.isArray()) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }

            return output;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS-KR] daily price error. ticker={}", krTicker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 국내 일봉 JsonNode -> DailyPriceItem 매핑
     */
    private DailyPriceItem toKorDailyItem(JsonNode node) {
        String dateStr = node.path("stck_bsop_date").asText(null);
        LocalDate date = LocalDate.parse(dateStr, KIS_DATE);

        BigDecimal open = new BigDecimal(node.path("stck_oprc").asText("0"));
        BigDecimal close = new BigDecimal(node.path("stck_clpr").asText("0"));
        BigDecimal high = new BigDecimal(node.path("stck_hgpr").asText("0"));
        BigDecimal low = new BigDecimal(node.path("stck_lwpr").asText("0"));
        BigDecimal volume = new BigDecimal(node.path("acml_vol").asText("0"));

        return new DailyPriceItem(
                date,
                open,
                close,
                high,
                low,
                volume
        );
    }

    /**
     * 해외 일별 시세 조회 (여러 번 호출해서 합)
     */
    private List<DailyPriceItem> getUsDailyPrices(Security security, LocalDate from, LocalDate to) {

        // 티커, 거래소 추출
        String ticker = security.getTicker();
        String excd = resolveExcd(security.getExchange());

        String startDay = (from != null) ? from.format(KIS_DATE) : "00000000";
        String endDay = (to != null) ? to.format(KIS_DATE) : LocalDate.now().format(KIS_DATE);

        List<JsonNode> allRows = new ArrayList<>();
        String bymd = endDay;
        try {
            while (true) {
                // 1번 호출당 100회 제한
                JsonNode output = callUsDailyPriceApi(excd, ticker, bymd);

                if (!output.isArray() || output.isEmpty()) {
                    break;  // 더 이상 데이터 없음
                }

                // 첫 호출인 경우 -> 전부 추가, 이후 호출부터 맨 앞 1개는 중복이므로 제외
                if (allRows.isEmpty()) {
                    output.forEach(allRows::add);
                } else {
                    for (int i = 1; i < output.size(); i++) {
                        allRows.add(output.get(i));
                    }
                }

                // 가장 오래된 날짜
                String oldest = output.get(output.size() - 1).path("xymd").asText(null);

                if (oldest == null || oldest.isBlank()) {
                    break;
                }

                // startDay까지 내려왔으면 루프 종료
                if (oldest.compareTo(startDay) <= 0) {
                    break;
                }

                // 방어 코드: 더 이상 과거로 못 내려가면 중단
                if (oldest.equals(bymd)) {
                    break;
                }

                // 다음 호출: 이번에 받은 가장 오래된 날짜 기준
                bymd = oldest;
            }

            return allRows.stream()
                    .map(this::toUsDailyItem)
                    .filter(item ->
                            (from == null || !item.date().isBefore(from)) &&
                            (to == null || !item.date().isAfter(to))
                    )
                    .sorted(Comparator.comparing(DailyPriceItem::date))
                    .toList();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[KIS-US] daily price error. ticker={}", ticker, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * KIS 해외 일별 시세 API 한 번 호출 (최대 100건)
     */
    private JsonNode callUsDailyPriceApi(String excd, String ticker, String bymd) {

        // URL
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/dailyprice")
                .queryParam("AUTH", "")
                .queryParam("EXCD", excd)
                .queryParam("SYMB", ticker)
                .queryParam("GUBN", "0")
                .queryParam("BYMD", bymd)
                .queryParam("MODP", "0")
                .toUriString();

        // 헤더
        HttpHeaders headers = createHeaders(KisTrId.US_DAILY_PRICE);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

        JsonNode body = response.getBody();
        JsonNode output = (body == null) ? null : body.get("output2");

        if (output == null || !output.isArray()) {
            throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
        }

        return output;
    }

    /**
     * 해외 일봉 JsonNode -> DailyPriceItem 매핑
     */
    private DailyPriceItem toUsDailyItem(JsonNode node) {
        String dateStr = node.path("xymd").asText(null);
        LocalDate date = LocalDate.parse(dateStr, KIS_DATE);

        BigDecimal open = new BigDecimal(node.path("open").asText("0"));
        BigDecimal close = new BigDecimal(node.path("clos").asText("0"));
        BigDecimal high = new BigDecimal(node.path("high").asText("0"));
        BigDecimal low = new BigDecimal(node.path("low").asText("0"));
        BigDecimal volume = new BigDecimal(node.path("tvol").asText("0"));

        return new DailyPriceItem(
                date,
                open,
                close,
                high,
                low,
                volume
        );
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
