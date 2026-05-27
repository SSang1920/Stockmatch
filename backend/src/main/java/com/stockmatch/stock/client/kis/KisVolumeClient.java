package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class KisVolumeClient extends AbstractKisClient {

    public KisVolumeClient(RestTemplate restTemplate, KisTokenProvider kisTokenProvider) {
        super(restTemplate, kisTokenProvider);
    }

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
                    .queryParam("FID_INPUT_ISCD", "0001")
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
            HttpHeaders headers = createHeaders(KisTrId.KR_RANK_VOLUME);
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
            if (body == null || !"0".equals(body.getRtCd())) {
                log.warn("[KIS-RANK] 거래량 랭킹 API 오류: {}", (body != null ? body.getMsg1() : "No Body"));
                return Collections.emptyList();
            }

            return body.getOutput();

        } catch (Exception e) {
            log.error("[KIS-RANK] Domestic volume rank error", e);
            return Collections.emptyList();
        }
    }

    /**
     * 해외 거래량 상위 조회
     */
    public List<KisOverseasVolumeItem> getOverseasVolumeRank(String excd) {
        try {
            // URL 생성
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-stock/v1/ranking/trade-vol")
                    .queryParam("KEYB", "")
                    .queryParam("AUTH", "")
                    .queryParam("EXCD", excd)
                    .queryParam("NDAY", "0")
                    .queryParam("PRC1", "")
                    .queryParam("PRC2", "")
                    .queryParam("VOL_RANG", "0")
                    .toUriString();

            // 헤더 생성
            HttpHeaders headers = createHeaders(KisTrId.US_RANK_VOLUME);
            headers.set("custtype", "P");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // API 호출
            ResponseEntity<KisOverseasVolumeRankResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KisOverseasVolumeRankResponse.class
            );

            KisOverseasVolumeRankResponse body = response.getBody();
            if (body == null || body.getOutput2() == null) {
                return Collections.emptyList();
            }

            return body.getOutput2();

        } catch (Exception e) {
            log.error("[KIS-RANK] Overseas volume rank error (Market: {})", excd, e);
            return Collections.emptyList();
        }
    }

    // ====== 내부 DTO (국내용) ======
    @Getter
    public static class KisVolumeRankResponse {
        @JsonProperty("output")
        private List<KisVolumeItem> output;
        @JsonProperty("rt_cd") private String rtCd;
        @JsonProperty("msg1") private String msg1;
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

    // ===== 내부 DTO (해외용) ======
    @Getter
    public static class KisOverseasVolumeRankResponse {
        @JsonProperty("output2")
        private List<KisOverseasVolumeItem> output2;
    }

    @Getter
    public static class KisOverseasVolumeItem {
        @JsonProperty("rank") private String rank;                      // 순위
        @JsonProperty("rsym") private String rsym;                      // 티커
        @JsonProperty("ename") private String ename;                    // 영문 종목명
        @JsonProperty("name") private String name;                      // 한글 종목명
        @JsonProperty("last") private String last;                      // 현재가
        @JsonProperty("diff") private String diff;                      // 대비
        @JsonProperty("rate") private String rate;                      // 등락률
        @JsonProperty("tvol") private String tvol;                      // 거래량
        @JsonProperty("excd") private String excd;                      // 거래소 코드
    }
}
