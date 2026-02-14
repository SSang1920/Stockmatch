package com.stockmatch.stock.client.kis;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalMinutePriceClient;
import com.stockmatch.stock.client.kis.dto.MinutePriceItem;
import com.stockmatch.stock.domain.Exchange;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisMinutePriceClient implements ExternalMinutePriceClient {

    private final SecurityRepository securityRepository;
    private final KisKorMinuteClient korClient;
    private final KisUsMinuteClient usClinet;

    @PreDestroy
    public void shutdown() {
        korClient.shutdown();
    }

    @Override
    public List<MinutePriceItem> getMinutePrices(String ticker) {
        // DB에서 종목 찾아서 국내/해외 거래소 확인
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        long start = System.currentTimeMillis();
        List<MinutePriceItem> result;

        if (security.isKorean()) {
            result = korClient.getMinutePrices(normalizeKrTicker(security.getTicker()));
        } else {
            result = usClinet.getMinutePrices(security.getTicker(), resolveExcd(security.getExchange()));
        }

        long end = System.currentTimeMillis();
        log.info("[Chart] ticker={} took={}ms size={}", ticker, (end - start), result.size());

        return result;
    }

    /**
     * 티커 정규화: 숫자만 들어올경우 6자리 숫자 변경
     */
    private String normalizeKrTicker(String ticker) {
        if (ticker == null) return null;

        ticker = ticker.trim();
        if (ticker.matches("\\d+")) {
            return String.format("%06d", Integer.parseInt(ticker));
        }
        return ticker;
    }

    /**
     * DB Exchange -> KIS EXCD 코드 매핑
     */
    private String resolveExcd(Exchange exchange) {
        if (exchange == null) return "NAS"; // 기본값

        return switch (exchange) {
            case NASDAQ -> "NAS";
            case NYSE -> "NYS";
            case AMEX -> "AMS";
            default -> "NAS";
        };
    }
}
