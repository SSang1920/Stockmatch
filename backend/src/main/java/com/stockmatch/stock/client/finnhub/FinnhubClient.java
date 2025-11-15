package com.stockmatch.stock.client.finnhub;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.client.ExternalPriceClient;
import com.stockmatch.stock.client.finnhub.dto.FinnhubSymbolProfile;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.dto.FinnhubQuoteResponse;
import com.stockmatch.stock.dto.Region;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component("finnhubClient")
@RequiredArgsConstructor
public class FinnhubClient implements ExternalPriceClient {

    private final RestTemplate restTemplate;

    @Value("${finnhub.base-url}")
    private String baseUrl;

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Override
    public StockPriceResponse getRealtime(String region, String ticker) {
        if (!"US".equalsIgnoreCase(region)) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_REGION);
        }

        return getQuote(ticker);
    }

    /**
     * 해외 단일 종목 현재가 조회 (표준 DTO)
     */
    public StockPriceResponse getQuote(String symbol) {
        FinnhubQuoteResponse raw = getQuoteRaw(symbol);
        FinnhubSymbolProfile profile = getUsSymbolProfile(symbol);

        String name = (profile != null && profile.name() != null && !profile.name().isBlank())
                ? profile.name()
                : symbol;

        return toStockPrice(symbol, raw).toBuilder().name(name).build();
    }

    /**
     * Finnhub 원본 응답 조회
     */
    public FinnhubQuoteResponse getQuoteRaw(String symbol) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl + "/quote")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            FinnhubQuoteResponse raw = restTemplate.getForObject(url, FinnhubQuoteResponse.class);
            if (raw == null) {
                throw new BusinessException(ErrorCode.UPSTREAM_DATA_EMPTY);
            }
            return raw;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * 해외 종목 기본 정보 조회
     */
    public FinnhubSymbolProfile getUsSymbolProfile(String symbol) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl + "/stock/profile2")
                    .queryParam("symbol", symbol)
                    .queryParam("token", apiKey)
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null || body.isEmpty()) {
                return null;
            }

            String ticker = asString(body.get("ticker"), symbol);
            String name = asString(body.get("name"), symbol);
            String currency = asString(body.get("currency"), Currency.USD.name());
            String exchange = asString(body.get("exchange"), Exchange.NASDAQ.name());

            return new FinnhubSymbolProfile(ticker, name, currency, exchange);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /**
     * Object 타입 값 -> String 타입 변환
     */
    private String asString(Object value, String defaultValue) {
        if (value instanceof String s && !s.isBlank()) {
            return s;
        }
        return defaultValue;
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
