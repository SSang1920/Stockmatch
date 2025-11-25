package com.stockmatch.stock.importer;

import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.domain.SecurityType;
import com.stockmatch.stock.repository.SecurityRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KrxMasterService {

    private final SecurityRepository securityRepository;

    @Transactional
    public int upsertKrxKospi(String ticker, String name) {
        // 정규화
        final String normTicker = normalizeTicker(ticker);
        final String normName = safe(name);

        var opt = securityRepository.findByTickerAndMarket(normTicker, Market.KR);
        Security security = opt.orElseGet(() -> Security.builder()
                .market(Market.KR)
                .exchange(Exchange.KOSPI)
                .currency(Currency.KRW)
                .type(SecurityType.STOCK)
                .ticker(normTicker)
                .build());

        security.updateName(normName);

        securityRepository.save(security);
        return 1;
    }

    @Transactional
    public int upsertKrxKosdaq(String ticker, String name) {
        // 정규화
        final String normTicker = normalizeTicker(ticker);
        final String normName = safe(name);

        var opt = securityRepository.findByTickerAndMarket(normTicker, Market.KR);
        Security security = opt.orElseGet(() -> Security.builder()
                .market(Market.KR)
                .exchange(Exchange.KOSDAQ)
                .currency(Currency.KRW)
                .type(SecurityType.STOCK)
                .ticker(normTicker)
                .build());

        security.updateName(normName);

        securityRepository.save(security);
        return 1;
    }

    private String normalizeTicker(String ticker) {
        if (ticker == null) return null;
        ticker = ticker.trim();

        // 숫자 6자리 left pad
        if (ticker.matches("\\d+")) {
            return String.format("%06d", Integer.parseInt(ticker));
        }
        return ticker;
    }

    private String safe(String s) {
        return s == null ? null : s.trim();
    }
}
