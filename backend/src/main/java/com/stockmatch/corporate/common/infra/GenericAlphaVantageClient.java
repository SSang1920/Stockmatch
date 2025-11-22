package com.stockmatch.corporate.common.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenericAlphaVantageClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${alphavantage.base-url}")
    private String baseUrl;

    /**
     * Alpha Vantage API에 데이터를 요청하는 범용 메서드
     *
     * @param function     API 기능 (예: "OVERVIEW", "EARNINGS")
     * @param symbol       조회할 기업 티커
     * @param apiKey       사용자의 API 키
     * @param dto 변환할 DTO 클래스(예: CompanyOverviewDto.class)
     * @param <T>          반환받고 싶은 데이터의 타입
     * @return             변환된 DTO 객체
     */
    public <T> T fetchData(
            String function,
            String symbol,
            String apiKey,
            Class<T> dto
    ) {
        String apiFunction = function.toUpperCase();
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/query")
                .queryParam("function", apiFunction)
                .queryParam("symbol", symbol)
                .queryParam("apikey", apiKey)
                .build(true).toUri();

        log.info("Requesting {} from Alpha Vantage for symbol: {}", function, symbol);

        Map<String, Object> rawResponse = restTemplate.getForObject(uri, Map.class);

        // 안내문 또는 빈 객체 확인
        if (rawResponse == null || rawResponse.isEmpty() || rawResponse.containsKey("Information") || rawResponse.containsKey("Note")) {
            log.warn("API returned an info/error message or empty response for {}: {}", symbol, rawResponse);
            // 잘못된 데이터가 캐시에 저장되는 것을 원천 방지
            throw new BusinessException(ErrorCode.EXTERNAL_API_DATA_NOT_FOUND);
        }

        // JSON -> DTO로 변환
        return objectMapper.convertValue(rawResponse, dto);
    }
}
