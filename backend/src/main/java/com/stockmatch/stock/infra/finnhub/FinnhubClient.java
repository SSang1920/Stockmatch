package com.stockmatch.stock.infra.finnhub;

import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FinnhubClient {

    private final RestTemplate restTemplate;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    @Value("${finnhub.api-key}")
    private String apiKey;

    /**
     * Finnhub 원본 응답 조회
     */
    public FinnhubQuoteResponse getQuoteRaw(String symbol) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/quote")
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .toUriString();

        FinnhubQuoteResponse raw = restTemplate.getForObject(url, FinnhubQuoteResponse.class);
        if (raw == null) {
            throw new IllegalStateException("Finnhub API 응답이 없습니다.");
        }

        return raw;
    }

    /**
     * 해외 단일 종목 현재가 조회 (표준 DTO)
     */
    public StockPriceResponse getQuote(String symbol) {
        FinnhubQuoteResponse raw = getQuoteRaw(symbol);
        String name = resolveNameBySymbol(symbol);

        return toStockPrice(symbol, raw).toBuilder().name(name).build();
    }

    /**
     * 심볼로 해외 종목명 조회
     */
    public String resolveNameBySymbol(String symbol) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/stock/profile2")
                .queryParam("symbol", symbol)
                .queryParam("token", apiKey)
                .toUriString();

        Map<?, ?> body = restTemplate.getForObject(url, Map.class);
        if (body == null) return symbol;

        Object name = body.get("name");
        return (name instanceof String s && !s.isBlank()) ? s : symbol;
    }

    /**
     * Finnhub 원본 -> stockPriceResponse 매핑
     */
    private StockPriceResponse toStockPrice(String symbol, FinnhubQuoteResponse raw) {
        double c = raw.getC();
        double pc = raw.getPc();
        double o = raw.getO();
        double h = raw.getH();
        double l = raw.getL();

        double changeRate = (pc == 0) ? 0.0 : (c - pc) / pc;

        return StockPriceResponse.builder()
                .region(Region.US)
                .ticker(symbol)
                .name(symbol)
                .currentPrice(c)
                .prevClose(pc)
                .openPrice(o)
                .highPrice(h)
                .lowPrice(l)
                .changeRate(changeRate)
                .build();
    }
}
