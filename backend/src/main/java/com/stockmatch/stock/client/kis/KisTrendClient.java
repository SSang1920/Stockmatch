package com.stockmatch.stock.client.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
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
public class KisTrendClient extends AbstractKisClient {

    public KisTrendClient(RestTemplate restTemplate,
                          KisTokenProvider kisTokenProvider) {
        super(restTemplate, kisTokenProvider);
    }

    // ===== 국내주식 급등/급락 =====
    /**
     * 국내 급등 순위 조회
     */
    public List<KisTrendRankItem> getDomesticGainers() {
        return getDomesticFluctuationRank("0");
    }

    /**
     * 국내 급락 순위 조회
     */
    public List<KisTrendRankItem> getDomesticLosers() {
        return getDomesticFluctuationRank("1");
    }

    /**
     * 국내 등락률 순위 API 호출
     */
    private List<KisTrendRankItem> getDomesticFluctuationRank(String sortCode) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/domestic-stock/v1/ranking/fluctuation")
                    .queryParam("FID_RSFL_RATE2", "")
                    .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                    .queryParam("FID_COND_SCR_DIV_CODE", "20170")
                    .queryParam("FID_INPUT_ISCD", "0000")
                    .queryParam("FID_RANK_SORT_CLS_CODE", sortCode)
                    .queryParam("FID_INPUT_CNT_1", "0")
                    .queryParam("FID_PRC_CLS_CODE", "1")
                    .queryParam("FID_INPUT_PRICE_1", "")
                    .queryParam("FID_INPUT_PRICE_2", "")
                    .queryParam("FID_VOL_CNT", "")
                    .queryParam("FID_TRGT_CLS_CODE", "0")
                    .queryParam("FID_TRGT_EXLS_CLS_CODE", "0")
                    .queryParam("FID_DIV_CLS_CODE", "0")
                    .queryParam("FID_RSFL_RATE1", "")
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.KR_RANK_FLUCTUATION);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KisTrendRankResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KisTrendRankResponse.class
            );

            KisTrendRankResponse body = response.getBody();
            if (body == null || body.getOutput() == null) {
                return Collections.emptyList();
            }

            return body.getOutput();

        } catch (Exception e) {
            log.error("Failed to fetch domestic fluctuation rank (sort={})", sortCode, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    // ===== 해외주식 급등/급락 =====

    /**
     * 해외 급등 순위 조회
     */
    public List<KisOverseasTrendRankItem> getOverseasGainers(String excd) {
        return getOverseasFluctuationRank("1", excd);
    }

    /**
     * 해외 급락 순위 조회
     */
    public List<KisOverseasTrendRankItem> getOverseasLosers(String excd) {
        return getOverseasFluctuationRank("0", excd);
    }

    /**
     * 해외 등락률 순위 API 호출
     */
    private List<KisOverseasTrendRankItem> getOverseasFluctuationRank(String sortCode, String excd) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(baseUrl + "/uapi/overseas-stock/v1/ranking/updown-rate")
                    .queryParam("KEYB", "")
                    .queryParam("AUTH", "")
                    .queryParam("EXCD", excd)
                    .queryParam("GUBN", sortCode)
                    .queryParam("NDAY", "0")
                    .queryParam("VOL_RANG", "0")
                    .toUriString();

            HttpHeaders headers = createHeaders(KisTrId.US_RANK_FLUCTUATION);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<KisOverseasTrendResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    KisOverseasTrendResponse.class
            );

            KisOverseasTrendResponse body = response.getBody();
            if (body == null || body.getOutput2() == null) {
                return Collections.emptyList();
            }

            return body.getOutput2();

        } catch (Exception e) {
            log.error("Failed to fetch overseas fluctuation rank (sort={})", sortCode, e);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    // ===== 내부 DTO =====
    /**
     * 국내 응답 DTO
     */
    @Getter
    public static class KisTrendRankResponse {
        @JsonProperty("output")
        private List<KisTrendRankItem> output;
    }

    @Getter
    public static class KisTrendRankItem {
        @JsonProperty("stck_shrn_iscd") private String ticker;      // 티커
        @JsonProperty("hts_kor_isnm") private String name;          // 종목명
        @JsonProperty("stck_prpr") private String currentPrice;     // 현재가
        @JsonProperty("prdy_vrss") private String changeAmount;     // 전일대비
        @JsonProperty("prdy_ctrt") private String changeRate;       // 등락률
        @JsonProperty("acml_vol") private String volume;            // 누적 거래량
        @JsonProperty("data_rank") private String rank;             // 순위
    }

    /**
     * 해외 응답 DTO
     */
    @Getter
    public static class KisOverseasTrendResponse {
        @JsonProperty("output2")
        private List<KisOverseasTrendRankItem> output2;
    }

    @Getter
    public static class KisOverseasTrendRankItem {
        @JsonProperty("rsym") private String ticker;            // 티커
        @JsonProperty("excd") private String excd;              // 거래소
        @JsonProperty("name") private String name;              // 한글 종목명
        @JsonProperty("ename") private String ename;            // 영어 종목명
        @JsonProperty("last") private String currentPrice;      // 현재가
        @JsonProperty("diff") private String changeAmount;      // 전일대비
        @JsonProperty("rate") private String changeRate;        // 등락률
        @JsonProperty("tvol") private String volume;            // 누적 거래량
        @JsonProperty("rank") private String rank;              // 순위
    }
}
