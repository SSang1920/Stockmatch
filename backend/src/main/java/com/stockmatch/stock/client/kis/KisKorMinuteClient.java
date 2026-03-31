package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.databind.JsonNode;
import com.stockmatch.stock.client.kis.dto.MinutePriceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class KisKorMinuteClient extends AbstractKisClient {

    private final KisApiHelper apiHelper;

    // 병렬 처리를 위한 스레드 풀
    private final ExecutorService executorService = Executors.newFixedThreadPool(6);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    public KisKorMinuteClient(RestTemplate restTemplate,
                              KisTokenProvider kisTokenProvider,
                              KisApiHelper apiHelper) {
        super(restTemplate, kisTokenProvider);
        this.apiHelper = apiHelper;
    }

    // 앱 종료 시 스레드 풀 정리
    public void shutdown() {
        executorService.shutdown();
    }

    /**
     * 국내 주식 분봉 조회 메인 로직
     */
    public List<MinutePriceItem> getMinutePrices(String ticker) {
        // 조회할 날짜 계산
        List<LocalDate> targetDates = getTargetDates();

        // 병렬 처리 결과 저장용 맵 생성
        ConcurrentHashMap<LocalDateTime, MinutePriceItem> dataMap = new ConcurrentHashMap<>();

        // 병렬 요청 시작
        List<CompletableFuture<Void>> futures = targetDates.stream()
                .map(date -> CompletableFuture.runAsync(() -> {
                    fetchDayData(ticker, date, dataMap);
                }, executorService))
                .toList();

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 정렬 및 반환
        List<MinutePriceItem> result = new ArrayList<>(dataMap.values());
        result.sort(Comparator.comparing(MinutePriceItem::dateTime));
        return result;
    }

    // 최근 2영업일 계산
    private List<LocalDate> getTargetDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate cursor = LocalDate.now();

        // 장 시작 전이면 어제 날짜로 조회
        if (LocalTime.now().isBefore(LocalTime.of(9, 0))) cursor = cursor.minusDays(1);

        while (dates.size() < 2) {
            DayOfWeek dow = cursor.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) dates.add(cursor);
            cursor = cursor.minusDays(1);
        }
        return dates;
    }

    // 하루치 데이터를 끝까지 긁어오는 로직
    private void fetchDayData(String ticker, LocalDate date, ConcurrentHashMap<LocalDateTime, MinutePriceItem> map) {
        String dateStr = date.format(DATE_FMT);
        String cursorTime = "999999"; // 기본값: 장 마감 이후

        // 오늘 장중이면 현재 시간부터 조회
        if (date.equals(LocalDate.now()) && LocalTime.now().isBefore(LocalTime.of(15, 30))) {
            cursorTime = LocalTime.now().format(TIME_FMT);
        }

        // 반복 조회 (최신 -> 09:00까지)
        for (int i = 0; i < 40; i++) {
            List<MinutePriceItem> chunk = fetchChunk(ticker, dateStr, cursorTime);
            if (chunk.isEmpty()) break;

            for (MinutePriceItem item : chunk) map.put(item.dateTime(), item);

            // 가장 과거 시간 확인
            LocalDateTime minTime = chunk.stream().map(MinutePriceItem::dateTime).min(LocalDateTime::compareTo).orElse(null);

            // 09:00 도달 시 종료
            if (minTime == null || !minTime.toLocalTime().isAfter(LocalTime.of(9, 0))) break;

            // 다음 커서 설정
            cursorTime = minTime.toLocalTime().minusMinutes(1).format(TIME_FMT);
        }
    }

    // 실제 API 호출 (1회)
    private List<MinutePriceItem> fetchChunk(String ticker, String date, String time) {
        return apiHelper.execute(() -> {
            String url = UriComponentsBuilder.fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_INPUT_ISCD", ticker)
                    .queryParam("FID_INPUT_HOUR_1", time).queryParam("FID_INPUT_DATE_1", date)
                    .queryParam("FID_PW_DATA_INCU_YN", "Y").queryParam("FID_FAKE_TICK_INCU_YN", "")
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.KR_MINUTE_PRICE);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
            return parseOutput(response.getBody().get("output2"));
        }, "[KIS-KR] " + ticker);
    }

    private List<MinutePriceItem> parseOutput(JsonNode output) {
        if (output == null || !output.isArray()) return List.of();
        List<MinutePriceItem> list = new ArrayList<>();
        for (JsonNode node : output) {
            String d = node.path("stck_bsop_date").asText();
            String t = node.path("stck_cntg_hour").asText();
            if (d.isBlank() || t.isBlank()) continue;

            list.add(new MinutePriceItem(
                    LocalDateTime.of(LocalDate.parse(d, DATE_FMT), LocalTime.parse(t, TIME_FMT)),
                    parseBigDecimal(node.path("stck_oprc").asText()),
                    parseBigDecimal(node.path("stck_hgpr").asText()),
                    parseBigDecimal(node.path("stck_lwpr").asText()),
                    parseBigDecimal(node.path("stck_prpr").asText()),
                    parseBigDecimal(node.path("cntg_vol").asText())
            ));
        }
        return list;
    }
}
