package com.stockmatch.stock.client.finnhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.stock.client.ExternalDailyPriceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinnhubDailyPriceClient implements ExternalDailyPriceClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Override
    public List<DailyPriceItem> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        long fromEpoch = toEpochSeconds(from);
        long toEpoch = toEpochSeconds(to.plusDays(1));

        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/stock/candle")
                .queryParam("symbol", ticker)
                .queryParam("resolution", "D")
                .queryParam("from", fromEpoch)
                .queryParam("to", toEpoch)
                .queryParam("token", apiKey)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("Finnhub daily price API failed. status={}, body={}", response.getStatusCode(), response.getBody());
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.path("s").asText();
            if (!"ok".equalsIgnoreCase(status)) {
                log.warn("Finnhub candle response not ok. s={}, body={}", status, response.getBody());
                return List.of();
            }

            JsonNode tArr = root.path("t");     // timestamp
            JsonNode oArr = root.path("o");     // open
            JsonNode cArr = root.path("c");     // close
            JsonNode hArr = root.path("h");     // high
            JsonNode lArr = root.path("l");     // low
            JsonNode vArr = root.path("v");     // volume

            int size = tArr.size();
            List<DailyPriceItem> result = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                long epochSec = tArr.get(i).asLong();
                LocalDate date = toLocalDate(epochSec);

                BigDecimal open = new BigDecimal(oArr.get(i).asText("0"));
                BigDecimal close = new BigDecimal(cArr.get(i).asText("0"));
                BigDecimal high = new BigDecimal(hArr.get(i).asText("0"));
                BigDecimal low = new BigDecimal(lArr.get(i).asText("0"));
                BigDecimal volume = new BigDecimal(vArr.get(i).asText("0"));

                result.add(new DailyPriceItem(
                        date,
                        open,
                        close,
                        high,
                        low,
                        volume
                ));
            }

            return result;

        } catch (Exception e) {
            log.error("Finnhub daily price parse error. body={}", response.getBody(), e);
            return List.of();
        }
    }

    /**
     * LocalDate -> UTC 기준 Epoch 초 변환
     */
    private long toEpochSeconds(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }

    /**
     * Epoch 초 -> LocalDate 변환
     */
    private LocalDate toLocalDate(long epochSeconds) {
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
    }
}
