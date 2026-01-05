package com.stockmatch.corporate.korea.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DartFinancialRawResponse {
    private String status;
    private String message;

    @JsonProperty("list")
    private List<RawAccountItem> accountItems;

    @Getter
    @NoArgsConstructor
    public static class RawAccountItem {
        @JsonProperty("rcept_no")
        private String rceptNo; //접수 번호

        @JsonProperty("reprt_code")
        private String reprtCode; //보고서 코드

        @JsonProperty("bsns_year")
        private String bsnsYear; //사업 연도

        @JsonProperty("corp_code")
        private String corpCode; //고유번호

        @JsonProperty("sj_div")
        private String sjDiv; //재무제표 구분 (BS: 재무상태표, IS: 손익계산서)

        @JsonProperty("sj_nm")
        private String sjNm; // 재무제표 명 (재무상태표, 손익계산서 등)

        @JsonProperty("account_nm")
        private String accountNm; // 계정 과목명 (예: 자산총계, 매출액)

        @JsonProperty("account_id")
        private String accountId; //계정 ID

        @JsonProperty("thstrm_nm")
        private String thstrmNm; //당기 명칭

        @JsonProperty("thstrm_amount")
        private String thstrmAmount; //당기 금액

        @JsonProperty("thstrm_add_amount")
        private String thstrmAddAmount; //당기 누적 금액

        @JsonProperty("frmtrm_amount")
        private String frmtrmAmount;

        @JsonProperty("frmtrm_add_amount")
        private String frmtrmAddAmount;

        @JsonProperty("bfefrmtrm_amount")
        private String bfefrmtrmAmount;

        @JsonProperty("ord")
        private String ord;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("fs_div")
        private String fsDiv;
    }
}
