package com.stockmatch.stock.importer;

import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.domain.SecurityType;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsMasterService {

    private final SecurityRepository securityRepository;

    /**
     * NASDAQ 종목 upsert
     */
    @Transactional
    public int upsertUsNasdaq(String ticker, String name, String englishName, String secType) {
        final String normTicker = normalizeTicker(ticker);
        final String normName = safe(name);
        final String normEngName = safe(englishName);

        SecurityType type = resolveSecurity(secType);

        var opt = securityRepository.findByTickerAndMarket(normTicker, Market.US);
        Security security = opt.orElseGet(() -> Security.builder()
                .market(Market.US)
                .exchange(Exchange.NASDAQ)
                .currency(Currency.USD)
                .type(type)
                .ticker(normTicker)
                .englishName(normEngName)
                .build());

        security.updateName(normName);
        security.updateEnglishName(normEngName);
        security.updateType(type);

        securityRepository.save(security);
        return 1;
    }

    /**
     * NYSE 종목 upsert
     */
    @Transactional
    public int upsertUsNyse(String ticker, String name, String englishName, String secType) {
        final String normTicker = normalizeTicker(ticker);
        final String normName = safe(name);
        final String normEngName = safe(englishName);

        SecurityType type = resolveSecurity(secType);

        var opt = securityRepository.findByTickerAndMarket(normTicker, Market.US);
        Security security = opt.orElseGet(() -> Security.builder()
                .market(Market.US)
                .exchange(Exchange.NYSE)
                .currency(Currency.USD)
                .type(type)
                .ticker(normTicker)
                .englishName(normEngName)
                .build());

        security.updateName(normName);
        security.updateEnglishName(normEngName);
        security.updateType(type);

        securityRepository.save(security);
        return 1;
    }

    /**
     * 미국 종목 티커 정규화
     */
    private String normalizeTicker(String ticker) {
        if (ticker == null) return null;
        return ticker.trim().toUpperCase();
    }

    private String safe(String s) {
        return s == null ? null : s.trim();
    }

    private SecurityType resolveSecurity(String secType) {

        // 기본값: 주식
        if (secType == null || secType.isBlank()) {
            return SecurityType.STOCK;
        }

        int st;
        try {
            st = Integer.parseInt(secType.trim());
        } catch (NumberFormatException e) {
            return SecurityType.STOCK;
        }

        // 1: Index, 2: Stock, 3: ETF, 4: Warrant
        switch (st) {
            case 3:
                return SecurityType.ETF;
            default:
                return SecurityType.STOCK;
        }
    }
}
