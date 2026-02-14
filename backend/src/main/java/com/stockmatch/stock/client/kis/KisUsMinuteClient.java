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

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Slf4j
@Component
public class KisUsMinuteClient extends AbstractKisClient {

    private final KisApiHelper apiHelper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private static final ZoneId US_ZONE = ZoneId.of("America/New_York");

    public KisUsMinuteClient(RestTemplate restTemplate,
                            KisTokenProvider kisTokenProvider,
                            KisApiHelper apiHelper) {
        super(restTemplate, kisTokenProvider);
        this.apiHelper = apiHelper;
    }

    /**
     * 해외 주식 분봉 조회 메인 로직
     */
    public List<MinutePriceItem> getMinutePrices(String ticker, String excd) {
        Map<LocalDateTime, MinutePriceItem> dataMap = new HashMap<>();

        // 조회 기준점 설정
        ZonedDateTime endEt = resolveEndTimeEt();
        String keyB = "";

        // 기준점이 현재와 2시간 이상 차이나면 keyB 설정
        if (Duration.between(endEt, ZonedDateTime.now(US_ZONE)).toHours() >= 2) {
            keyB = endEt.format(DATE_FMT) + endEt.toLocalTime().format(TIME_FMT);
        }

        // 20시간 전 데이터까지 확보
        ZonedDateTime targetStartEt = endEt.minusHours(20);
        String nextKey = "";

        for (int i = 0; i < 8; i++) {
            JsonNode body = fetchChunk(ticker, excd, nextKey, keyB);
            if (body == null) break;

            JsonNode output2 = body.get("output2");
            if (output2 == null || output2.isEmpty()) break;

            List<MinutePriceItem> chunk = parseOutput(output2);
            for (MinutePriceItem item : chunk) dataMap.put(item.dateTime(), item);

            LocalDateTime oldest = dataMap.keySet().stream().min(LocalDateTime::compareTo).orElse(null);
            if (oldest != null && oldest.atZone(US_ZONE).isBefore(targetStartEt)) break;

            String hasNext = body.path("output1").path("next").asText();
            if (!"1".equals(hasNext)) break;

            JsonNode last = output2.get(output2.size() - 1);
            keyB = last.path("kymd").asText() + String.format("%06d", last.path("khms").asInt());
            nextKey = "1";
        }

        List<MinutePriceItem> result = new ArrayList<>(dataMap.values());
        result.sort(Comparator.comparing(MinutePriceItem::dateTime));

        return result;
    }

    // 미국 시간 기준 종료 시점 계산
    private ZonedDateTime resolveEndTimeEt() {
        ZonedDateTime now = ZonedDateTime.now(US_ZONE);
        if (now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return now.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY)).with(LocalTime.of(20, 0));
        }
        if (now.toLocalTime().isBefore(LocalTime.of(4, 0))) {
            return now.minusDays(1).with(LocalTime.of(20, 0));
        }
        return now;
    }

    private JsonNode fetchChunk(String ticker, String excd, String next, String keyB) {
        return apiHelper.execute(() -> {
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

            ResponseEntity<JsonNode> res = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return res.getBody();
        }, "[KIS-US] " + ticker);
    }

    private List<MinutePriceItem> parseOutput(JsonNode output) {
        List<MinutePriceItem> list = new ArrayList<>();
        for (JsonNode node : output) {
            String d = node.path("kymd").asText();
            String t = node.path("khms").asText();
            if (d.isBlank() || t.isBlank()) continue;
            if (t.length() == 5) t = "0" + t;

            list.add(new MinutePriceItem(
                    LocalDateTime.of(LocalDate.parse(d, DATE_FMT), LocalTime.parse(t, TIME_FMT)),
                    parseBigDecimal(node.path("open").asText()),
                    parseBigDecimal(node.path("high").asText()),
                    parseBigDecimal(node.path("low").asText()),
                    parseBigDecimal(node.path("last").asText()),
                    parseBigDecimal(node.path("evol").asText())
            ));
        }
        return list;
    }
}
