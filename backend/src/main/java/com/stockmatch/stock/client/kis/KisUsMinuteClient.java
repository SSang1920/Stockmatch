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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class KisUsMinuteClient extends AbstractKisClient {

    private final KisApiHelper apiHelper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

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

        String keyB = "";
        String nextKey = "";
        int maxLoop = 12;

        // 지금까지 찾은 가장 오래된 시간을 기록하는 변수
        long globalMinTimestamp = Long.MAX_VALUE;

        log.info("[KIS-US] Start fetching for {} (MaxLoop: {})", ticker, maxLoop);

        for (int i = 0; i < maxLoop; i++) {
            JsonNode body = fetchChunk(ticker, excd, nextKey, keyB);
            if (body == null) break;

            JsonNode output2 = body.get("output2");
            if (output2 == null || output2.isEmpty()) break;

            List<MinutePriceItem> chunk = parseOutput(output2);
            if (!chunk.isEmpty()) {
                MinutePriceItem first = chunk.get(0);
                MinutePriceItem last = chunk.get(chunk.size() - 1);
                log.info("[KIS-US] Loop {}: Fetched {} items. Range: {} ~ {}",
                        i, chunk.size(), last.dateTime(), first.dateTime());
            }

            for (MinutePriceItem item : chunk) dataMap.put(item.dateTime(), item);

            String minDateTimeStr = null;
            long currentChunkMin = Long.MAX_VALUE;

            for (JsonNode node : output2) {
                String d = node.path("kymd").asText();
                String t = node.path("khms").asText();
                if (d.isBlank() || t.isBlank()) continue;

                // 시간 포맷 보정 (93000 -> 093000)
                if (t.length() == 5) t = "0" + t;

                long val = Long.parseLong(d + t);

                // 가장 작은 값 갱신
                if (val < currentChunkMin) {
                    currentChunkMin = val;
                    minDateTimeStr = d + t;
                }
            }

            // 유효한 날짜가 없으면 중단
            if (minDateTimeStr == null) {
                break;
            }

            // 만약 이번에 찾은 최저 시간이, 이전보다 미래거나 같으면 API 오류
            if (currentChunkMin >= globalMinTimestamp) {
                log.warn("[KIS-US] Loop {}: Time stuck or jumped forward! (Last: {}, Curr: {}). Stopping to prevent infinite loop.",
                        i, globalMinTimestamp, currentChunkMin);
                break; // 무한루프 방지
            }

            // 정상적으로 과거로 내려갔으면 KEYB 갱신
            globalMinTimestamp = currentChunkMin;
            keyB = minDateTimeStr;
            nextKey = "1";

            // 다음 페이지 존재 여부 확인
            String hasNext = body.path("output1").path("next").asText();
            if (!"1".equals(hasNext)) {
                log.info("[KIS-US] Loop {}: NEXT is not 1 ({}). Stop.", i, hasNext);
                break;
            }

            log.info("[KIS-US] Loop {}: Fetched {} items. Next KEYB: {}", i, chunk.size(), keyB);
        }

        List<MinutePriceItem> result = new ArrayList<>(dataMap.values());
        result.sort(Comparator.comparing(MinutePriceItem::dateTime));

        if (!result.isEmpty()) {
            log.info("[KIS-US] Done. Total: {} items. Range: {} ~ {}",
                    result.size(), result.get(0).dateTime(), result.get(result.size()-1).dateTime());
        }

        return result;
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
