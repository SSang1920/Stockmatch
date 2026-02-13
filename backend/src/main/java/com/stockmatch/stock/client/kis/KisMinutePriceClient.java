package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalMinutePriceClient;
import com.stockmatch.stock.client.kis.dto.MinutePriceItem;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class KisMinutePriceClient extends AbstractKisClient implements ExternalMinutePriceClient {

    private final SecurityRepository securityRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(6);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime US_PRE_OPEN = LocalTime.of(4, 0);
    private static final LocalTime US_AFTER_CLOSE = LocalTime.of(20, 0);

    private static final int MAX_RETRY = 3;
    private static final long BASE_BACKOFF_MS = 250L;

    private final Semaphore apiPermit = new Semaphore(4);
    private static final long MIN_CALL_GAP_MS = 60L;
    private volatile long lastCallAtMs = 0L;

    public KisMinutePriceClient(RestTemplate restTemplate,
                                KisTokenProvider kisTokenProvider,
                                SecurityRepository securityRepository) {
        super(restTemplate, kisTokenProvider);
        this.securityRepository = securityRepository;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public List<MinutePriceItem> getMinutePrices(String ticker) {
        // DB에서 종목 찾아서 국내/해외 거래소 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        long start = System.currentTimeMillis();
        List<MinutePriceItem> result;
        if (security.isKorean()) {
            result = getKorMinutePrices(normalizeKrTicker(security.getTicker()));
        } else {
            result = getUsMinutePrices(security);
        }

        long end = System.currentTimeMillis();
        log.info("Chart Fetch Time: {}ms", end - start);

        return result;
    }

    /**
     * 국내 주식 분봉 조회
     */
    private List<MinutePriceItem> getKorMinutePrices(String ticker) {
        // 조회할 최근 2일 영업일 날짜 리스트 생성
        List<LocalDate> targetDates = collectRecentBusinessDays(2);

        // 중복 제거
        ConcurrentHashMap<LocalDateTime, MinutePriceItem> dataMap = new ConcurrentHashMap<>();

        // 비동기 요청 (2일치 동시 요청)
        List<CompletableFuture<Void>> futures = targetDates.stream()
                .map(date -> CompletableFuture.runAsync(() -> {
                    fetchKorDayFullData(ticker, date, dataMap);
                }, executorService))
                .toList();

        // 모든 스레드가 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Map -> List 변환 및 정렬
        List<MinutePriceItem> allData = new ArrayList<>(dataMap.values());
        allData.sort(Comparator.comparing(MinutePriceItem::dateTime));

        log.info("[KIS-KR] ticker={} days={} items={}", ticker, targetDates, allData.size());

        return allData;
    }

    private List<LocalDate> collectRecentBusinessDays(int count) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = LocalDate.now();

        // 장 시작 전이면 전 영업일부터
        if (LocalTime.now().isBefore(LocalTime.of(9, 0))) {
            cursor = cursor.minusDays(1);
        }

        while (dates.size() < count) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                dates.add(cursor);
            }
            cursor = cursor.minusDays(1);

            if (cursor.isBefore(LocalDate.now().minusDays(20))) break;
        }

        return dates;
    }

    private void fetchKorDayFullData(String ticker, LocalDate targetDate, ConcurrentHashMap<LocalDateTime, MinutePriceItem> dataMap) {
        String dateStr = targetDate.format(DATE_FMT);

        // 시간 설정
        String cursorTime = "999999";
        if (targetDate.equals(LocalDate.now()) && LocalTime.now().isBefore(LocalTime.of(15, 30))) {
            cursorTime = LocalTime.now().format(TIME_FMT);
        }

        for (int loop = 0; loop < 40; loop++) {
            List<MinutePriceItem> chunk = fetchKorChunkWithRetry(ticker, dateStr, cursorTime);

            if (chunk.isEmpty()) break;

            // 맵에 저장
            for (MinutePriceItem item : chunk) {
                dataMap.put(item.dateTime(), item);
            }

            // 가장 과거 시간 찾기
            LocalDateTime minTime = chunk.stream()
                    .map(MinutePriceItem::dateTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            if (minTime == null) break;

            // 09:00 도달하면 종료
            if (!minTime.toLocalTime().isAfter(LocalTime.of(9, 0))) {
                break;
            }

            // 다음 커서: 가장 과거 바 - 1분
            cursorTime = minTime.toLocalTime().minusMinutes(1).format(TIME_FMT);
        }
    }

    private List<MinutePriceItem> fetchKorChunkWithRetry(String ticker, String dateStr, String timeStr) {
        return withRetry(() -> fetchKorChunk(ticker, dateStr, timeStr),
                "[KIS-KR] fetch retry ticker=" + ticker
                        + " date=" + dateStr + " time=" + timeStr);
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

        // 중복 제거
        Map<LocalDateTime, MinutePriceItem> dataMap = new HashMap<>();

        // ET 기준 끝시각 결정
        ZonedDateTime endEt = resolveUsEndTimeEt();

        // 장중이면 최신 값 받고, 장이 닫혔으면 endEt를 keyB로 설정
        String keyB = "";
        if (shouldAnchorKeyB(endEt)) {
            keyB = endEt.format(DATE_FMT) + endEt.toLocalTime().format(TIME_FMT);
        }

        // 프리장(5h) + 정규장(6.5h) + 애프터장(4h) = 15.5h
        ZonedDateTime startTargetEt = endEt.minusHours(16);

        String nextKey = "";
        int maxLoop = 8;

        log.info("[KIS-US] start ticker={} excd={} endEt={} startEt={} keyB={}", ticker, excd, endEt, startTargetEt, keyB);

        for (int i = 0; i < maxLoop; i++) {
            JsonNode body;
            try {
                // API 호출
                body = fetchUsChunkWithRetry(ticker, excd, nextKey, keyB);
            } catch (Exception e) {
                log.warn("[KIS-US] fetch error ticker={}, loop={}, msg={}",
                        ticker, i, e.getMessage(), e);
                break;
            }

            if (body == null) break;

            JsonNode output2 = body.get("output2");
            if (output2 == null || !output2.isArray() || output2.isEmpty()) {
                log.info("[KIS-US] empty output2 ticker={}, loop={}, next={}, keyB={}",
                        ticker, i, nextKey, keyB);
                break;
            }

            List<MinutePriceItem> chunk = parseUsOutput(output2);

            for (MinutePriceItem item : chunk) {
                dataMap.put(item.dateTime(), item);
            }

            JsonNode output1 = body.get("output1");
            String hasNext = (output1 != null) ? output1.path("next").asText() : "";

            if (!"1".equals(hasNext)) {
                break;
            }

            JsonNode lastNode = output2.get(output2.size() - 1);
            String lastDate = lastNode.path("kymd").asText();
            String lastTime = lastNode.path("khms").asText();

            if (lastDate.isBlank() || lastTime.isBlank()) break;

            // 6자리 보정
            if (lastTime.length() == 5) lastTime = "0" + lastTime;

            keyB = lastDate + lastTime;
            nextKey = "1";
        }

        // 맵 -> 리스트 변환 및 정렬
        List<MinutePriceItem> allData = new ArrayList<>(dataMap.values());
        allData.sort(Comparator.comparing(MinutePriceItem::dateTime));

        // 디버깅용
        if (!allData.isEmpty()) {
            LocalDateTime min = allData.get(0).dateTime();
            LocalDateTime max = allData.get(allData.size() - 1).dateTime();

            // ET로 변환해서도 찍어보면 기준시간이 무엇인지 바로 드러남
            ZonedDateTime minEt = min.atZone(ZoneId.systemDefault()).withZoneSameInstant(NY_ZONE);
            ZonedDateTime maxEt = max.atZone(ZoneId.systemDefault()).withZoneSameInstant(NY_ZONE);

            log.info("[KIS-US] ticker={} count={} min={} (ET:{}) max={} (ET:{})",
                    ticker, allData.size(), min, minEt.toLocalDateTime(), max, maxEt.toLocalDateTime());
        }

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
