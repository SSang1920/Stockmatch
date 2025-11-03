package com.stockmatch.stock.infra.finnhub;

import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class FinnhubClient {

    private final RestTemplate restTemplate;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    @Value("${finnhub.api-key}")
    private String apiKey;

    /**
     * 해외 단일 종목 현재가 조회
     */
    public StockPriceResponse getQuote(String symbol) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/quote")
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .toUriString();

        FinnhubQuoteResponse raw = restTemplate.getForObject(url, FinnhubQuoteResponse.class);

        if (raw == null) {
            throw new IllegalStateException("Finnhub API 응답이 없습니다.");
        }

        // FinnhubQuoteResponse -> StockPriceResponse 변환
        return StockPriceResponse.builder()
                .symbol(symbol)
                .name(symbol)
                .currentPrice(raw.getC())
                .prevClose(raw.getPc())
                .openPrice(raw.getO())
                .highPrice(raw.getH())
                .lowPrice(raw.getL())
                .changeRate((raw.getC() - raw.getPc()) / raw.getPc() * 100.0)
                .build();
    }
}
