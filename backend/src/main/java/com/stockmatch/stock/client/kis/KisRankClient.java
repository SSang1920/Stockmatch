package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisRankClient {

    private final RestTemplate restTemplate;
    private final KisTokenProvider kisTokenProvider;

    @Value("${kis.base-url}")
    private String baseUrl;

    @Value("${kis.app-key}")
    private String appKey;

    @Value("${kis.app-secret}")
    private String appSecret;

    private static final String TR_ID_KR_VOLUME_RANK = "FHPST01710000";

    /**
     * 국내 거래량 상위 조회
     */
    public List<KisVolumeItem> getDomesticVolumeRank() {
        try {
            // URL 생성
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/quotations/volume-rank")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                    .queryParam("FID_INPUT_ISCD", "0000")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("FID_BLNG_CLS_CODE", "0")
                    .queryParam("FID_TRGT_CLS_CODE", "111111111")
                    .queryParam("FID_TRGT_EXLS_CLS_CODE", "000000")
                    .queryParam("FID_INPUT_PRICE_1", "")
                    .queryParam("FID_INPUT_PRICE_2", "")
                    .queryParam("FID_VOL_CNT", "")
                    .queryParam("FID_INPUT_DATE_1", "")
                    .toUriString();

            // 헤더 생성
            HttpHeaders headers = createHeaders(TR_ID_KR_VOLUME_RANK);
            headers.set("custtype", "P");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // API 호출
            ResponseEntity<KisVolumeRankResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KisVolumeRankResponse.class
            );

            KisVolumeRankResponse body = response.getBody();
            if (body == null || body.getOutput() == null) {
                return Collections.emptyList();
            }

            return body.getOutput();

        } catch (Exception e) {
            log.error("[KIS-RANK] Domestic volume rank error", e);
            return Collections.emptyList();
        }
    }

    /**
     * (공통) 헤더 생성
     */
    private HttpHeaders createHeaders(String trId) {
        String accessToken = kisTokenProvider.getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", trId);

        return headers;
    }

    // ====== 내부 DTO ======
    @Getter
    public static class KisVolumeRankResponse {
        @JsonProperty("output")
        private List<KisVolumeItem> output;
    }

    @Getter
    public static class KisVolumeItem {
        @JsonProperty("hts_kor_isnm") private String htsKorIsnm;        // 종목명
        @JsonProperty("mksc_shrn_iscd") private String mkscShrnIscd;    // 티커
        @JsonProperty("stck_prpr") private String stckPrpr;             // 현재가
        @JsonProperty("prdy_vrss") private String prdyVrss;             // 전일대비
        @JsonProperty("prdy_ctrt") private String prdyCtrt;             // 등락률
        @JsonProperty("acml_vol") private String acmlVol;               // 누적 거래량
        @JsonProperty("data_rank") private String dataRank;             // 순위
    }
}
