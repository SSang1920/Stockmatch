package com.stockmatch.stock.client.kis;

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    @Value("${kis.tr-id.kr.daily-price}")
    private String trId;

    /**
     * 특정 티커의 기간동안 일별 시세 조회
     */
    @Override
    public List<DailyPriceItem> getDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // 날짜 변환
        String fromStr = from.format(KIS_DATE);
        String toStr = to.format(KIS_DATE);

        // URL
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                .queryParam("FID_COND_MRKT_DIV_CODE", "J")   // 조건 시장 분류 코드(J:KRX, NX:NXT, UN:통합)
                .queryParam("FID_INPUT_ISCD", ticker)               // 종목코드
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
        headers.set("tr_id", trId);
        headers.set("custtype", "P");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("KIS daily price API failed. status={}, body={}", response.getStatusCode(), response.getBody());
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode rows = root.path("output2");

            List<DailyPriceItem> result = new ArrayList<>();

            for (JsonNode row : rows) {
                // 날짜
                LocalDate date = LocalDate.parse(row.path("stck_bsop_date").asText(), KIS_DATE);

                // 시가/종가/고가/저가
                BigDecimal open = new BigDecimal(row.path("stck_oprc").asText("0"));
                BigDecimal close = new BigDecimal(row.path("stck_clpr").asText("0"));
                BigDecimal high = new BigDecimal(row.path("stck_hgpr").asText("0"));
                BigDecimal low = new BigDecimal(row.path("stck_lwpr").asText("0"));

                // 거래량
                BigDecimal volume = new BigDecimal(row.path("acml_vol").asText("0"));

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
            log.error("KIS daily price parse error. body={}", response.getBody(), e);
            return List.of();
        }
    }
}
