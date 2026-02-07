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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
        List<MinutePriceItem> allData = new ArrayList<>();

        // 날짜 결정
        LocalDate targetDate = LocalDate.now();
        DayOfWeek dayOfWeek = targetDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SUNDAY) targetDate = targetDate.minusDays(2);
        else if (dayOfWeek == DayOfWeek.SATURDAY) targetDate = targetDate.minusDays(1);
        else if (LocalTime.now().isBefore(LocalTime.of(9, 0))) {
            // 평일인데 9시 이전이면 어제 데이터
            targetDate = targetDate.minusDays(1);
            if (targetDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                targetDate = targetDate.minusDays(2);
            } else if (targetDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
                targetDate = targetDate.minusDays(1);
            }
        }

        String targetDateStr = targetDate.format(DATE_FMT);
        log.info("Fetching KR Minute Chart for Date: {}", targetDateStr);

        // 시간 루프 설정 (장 마감부터 09:00까지 역순 조회)
        String cursorTime = "999999";
        if (targetDate.equals(LocalDate.now()) && LocalTime.now().isBefore(LocalTime.of(15, 30))) {
            cursorTime = LocalTime.now().format(TIME_FMT);
        }

        boolean hasMore = true;

        while (hasMore) {
            try {
                // API 호출
                List<MinutePriceItem> chunk = fetchKorChunk(ticker, targetDateStr, cursorTime);

                if (chunk.isEmpty()) {
                    hasMore = false;
                    break;
                }

                allData.addAll(chunk);

                // 다음 조회를 위한 시간 계산
                // 가져온 데이터 중 가장 과거 시간 찾기
                LocalDateTime minTime = chunk.stream()
                        .map(MinutePriceItem::dateTime)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

                // 9시 데이터까지 다 오거나 더 이상 없으면 종료
                if (minTime == null || !minTime.toLocalTime().isAfter(LocalTime.of(9, 0))) {
                    hasMore = false;
                } else {
                    // 가장 과거 시간의 1분 전 시간을 다음 커서로 설정
                    cursorTime = minTime.toLocalTime().minusMinutes(1).format(TIME_FMT);

                    // API 부하 방지 딜레이
                    Thread.sleep(50);
                }

            } catch (Exception e) {
                log.error("[KIS-KR] Error fetching chunk", e);
                hasMore = false;
            }
        }

        // 시간 오름차순 정렬 (09:00 -> 15:30)
        allData.sort(Comparator.comparing(MinutePriceItem::dateTime));

        return allData;
    }

    private List<MinutePriceItem> fetchKorChunk(String ticker, String dateStr, String timeStr) {
        try {
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
            return List.of();
        }
    }

    /**
     * 해외 주식 분봉 조회
     */
    private List<MinutePriceItem> getUsMinutePrices(Security security) {
        String ticker = security.getTicker();
        String excd = resolveExcd(security.getExchange());

        Map<LocalDateTime, MinutePriceItem> dataMap = new HashMap<>();

        String nextKey = "";
        String keyB = "";

        // 현재 시간 확인
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        log.info("[KIS-US] Fetching minute data for {} (Day: {}, Time: {})", ticker, dayOfWeek, time);

        // 월요일 오후 5시 이전이면 주말로 작동
        boolean isWeekendState = (dayOfWeek == DayOfWeek.SATURDAY) ||
                (dayOfWeek == DayOfWeek.SUNDAY) ||
                (dayOfWeek == DayOfWeek.MONDAY && time.isBefore(LocalTime.of(17, 30)));

        if (isWeekendState) {
            LocalDate targetDate = now.toLocalDate();
            while (targetDate.getDayOfWeek() != DayOfWeek.SATURDAY) {
                targetDate = targetDate.minusDays(1);
            }

            // 토요일 오전 6시를 정규장 마감으로 설정
            keyB = targetDate.format(DATE_FMT) + "060000";
            log.info("[KIS-US] Weekend/Monday-Morning Mode Activated! Force KEYB: {}", keyB);
        } else {
            log.info("[KIS-US] Normal Mode (Live Market or Weekday). KEYB: Empty");
        }


        // 한 번 호출: 120건 (960분 = 16시간)
        // 프리장(5h) + 정규장(6.5h) + 애프터장(4h) = 15.5h
        int maxLoop = 8;

        for (int i = 0; i < maxLoop; i++) {
            try {
                // API 제한
                Thread.sleep(50);

                // API 호출
                JsonNode body = fetchUsChunk(ticker, excd, nextKey, keyB);
                if (body == null) break;

                // 데이터 파싱
                JsonNode output2 = body.get("output2");
                if (output2 != null && output2.isArray() && !output2.isEmpty()) {
                    List<MinutePriceItem> chunk = parseUsOutput(output2);

                    for (MinutePriceItem item : chunk) {
                        dataMap.put(item.dateTime(), item);
                    }

                    // 다음 호출을 위한 키 추출
                    JsonNode output1 = body.get("output1");
                    String hasNext = (output1 != null) ? output1.path("next").asText() : "";

                    if ("1".equals(hasNext)) {
                        // KEYB는 이번 응답의 마지막 데이터의 시간값
                        JsonNode lastNode = output2.get(output2.size() - 1);
                        String lastDate = lastNode.path("kymd").asText();
                        String lastTime = lastNode.path("khms").asText();

                        if (lastDate.isBlank() || lastTime.isBlank()) break;

                        keyB = lastDate + lastTime;
                        nextKey = "1";
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } catch (Exception e) {
                log.error("[KIS-US] Error fetching chunk", e);
                break;
            }
        }

        // 맵 -> 리스트 변환 및 정렬
        List<MinutePriceItem> allData = new ArrayList<>(dataMap.values());
        allData.sort(Comparator.comparing(MinutePriceItem::dateTime));

        return allData;
    }

    private JsonNode fetchUsChunk(String ticker, String excd, String next, String keyB) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/uapi/overseas-price/v1/quotations/inquire-time-itemchartprice")
                .queryParam("AUTH", "")
                .queryParam("EXCD", excd)
                .queryParam("SYMB", ticker)
                .queryParam("NMIN", "1")
                .queryParam("PINC", "1")
                .queryParam("NEXT", next)
                .queryParam("NREC", "120")
                .queryParam("FILL", "")
                .queryParam("KEYB", keyB)
                .toUriString();

        HttpHeaders headers = createHeaders(KisTrId.US_MINUTE_PRICE);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
        return response.getBody();
    }

    private List<MinutePriceItem> parseUsOutput(JsonNode output2) {
        List<MinutePriceItem> list = new ArrayList<>();
        for (JsonNode node : output2) {
            String dateRaw = node.path("kymd").asText();
            String timeRaw = node.path("khms").asText();

            if (dateRaw.isBlank() || timeRaw.isBlank()) continue;

            // 6자리 시간값 포맷
            if (timeRaw.length() == 5) timeRaw = "0" + timeRaw;

            LocalDate date = LocalDate.parse(dateRaw, DATE_FMT);
            LocalTime time = LocalTime.parse(timeRaw, TIME_FMT);
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            BigDecimal open = new BigDecimal(node.path("open").asText("0"));
            BigDecimal high = new BigDecimal(node.path("high").asText("0"));
            BigDecimal low = new BigDecimal(node.path("low").asText("0"));
            BigDecimal close = new BigDecimal(node.path("last").asText("0"));
            BigDecimal volume = new BigDecimal(node.path("evol").asText("0"));

            list.add(new MinutePriceItem(dateTime, open, high, low, close, volume));
        }

        return list;
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
