package com.stockmatch.exchangeRate.infra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BokApiClient {

    private final RestTemplate restTemplate;

    @Value("${bok.api.key}")
    private String apiKey;

    // 한국은행 API URL 형식
    // 서비스명: StatisticSearch, API키, 파일타입: json, 언어: kr, 시작요청~종료요청: 1/1
    // 통계표코드: 036Y001 (주요국 통화의 대원화 환율), 주기: DD (일별)
    // 조회시작일, 조회종료일
    // 항목코드: 0000001 (미국 달러)
    private static final String BOK_API_URL = "https://ecos.bok.or.kr/api/StatisticSearch/{apiKey}/json/kr/{startCount}/{endCount}/{statisticCode}/{period}/{startDate}/{endDate}/{itemCode}";

    /**
     * 한국은행 API로부터 특정 날짜의 원 / 달러 환율 조회
     * @param date 조회할 날짜
     * @return 환율, 실패시 null
     */
    public BigDecimal fetchUsdToKrwRate(LocalDate date){
        //API 요구 형식에 맞춰 날짜 변환
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // API 키와 날짜를 URL 빈칸에 채워 최종 요청 주소 생성
        String url = BOK_API_URL
                .replace("{apiKey}", apiKey)
                .replace("{startCount}", "1")
                .replace("{endCount}", "10")
                .replace("{statisticCode}", "731Y001") //통계표 코드
                .replace("{period}", "D")             // 주기 코드
                .replace("{startDate}", dateStr)
                .replace("{endDate}", dateStr)
                .replace("{itemCode}", "0000001");    // 미국 달러

        try{

            BokApiResponse response = restTemplate.getForObject(url, BokApiResponse.class);

            if(response != null && response.statisticSearch() != null && !response.statisticSearch().row().isEmpty()){
                String rateStr = response.statisticSearch().row.get(0).dataValue();
                log.info("Successfully fetched Usd/KRW rate from BOK API for date {}: {}", date, rateStr);

                return new BigDecimal(rateStr);
            }
            // API호출 성공했지만 해당 날짜의 환율이 없는 경우
            log.warn("No exchange rate data found from BOK API for date: {}. ", date);
            return null;
        } catch (Exception e){
            //API 호출을 실패한 경우
            log.error("Failed to fetch exchange rate from BOK API for date: " + date, e);
            return null;
        }
    }

    //StatisticSearch 객체 포함
    private record BokApiResponse(
            @JsonProperty("StatisticSearch")
            StatisticSearch statisticSearch
    ) {}

    // row 배열 포함
    private record StatisticSearch(
            List<Row> row
    ) {}

    // row 안쪽의 항목 구조, DATA_VALUE 필드 포함
    private record Row(
            @JsonProperty("DATA_VALUE")
            String dataValue
    ) {}
}


