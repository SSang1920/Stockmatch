package com.stockmatch.stock.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.stock.client.ExternalDailyPriceClient;
import com.stockmatch.stock.client.finnhub.FinnhubDailyPriceClient;
import com.stockmatch.stock.client.kis.KisDailyPriceClient;
import com.stockmatch.stock.domain.DailyPrice;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.DailyPriceRepository;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPriceSyncService {

    private final KisDailyPriceClient kisDailyPriceClient;
    private final FinnhubDailyPriceClient finnhubDailyPriceClient;
    private final SecurityRepository securityRepository;
    private final DailyPriceRepository dailyPriceRepository;

    /**
     * 특정 티커의 기간별 일일 시세를 외부 API에서 가져와 daily_price 테이블에 저장/업데이트
     */
    @Transactional
    public void syncDailyPrices(String ticker, LocalDate from, LocalDate to) {

        // 종목 조회
        Security security = securityRepository.findByTicker(ticker)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 종목/시장에 따라 외부 API에서 데이터 가져오기
        List<ExternalDailyPriceClient.DailyPriceItem> items =
                fetchForSecurity(security, from, to);

        // daily_price upsert
        for (var item : items) {

            // 기존 데이터 있는지 확인
            DailyPrice entity = dailyPriceRepository.findBySecurityIdAndDate(security.getId(), item.date())
                    .orElse(null);

            if (entity == null) {
                // 신규 생성
                entity = new DailyPrice(
                        null,
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            } else {
                // 업데이트
                entity = new DailyPrice(
                        entity.getId(),
                        security,
                        item.date(),
                        item.openPrice(),
                        item.closePrice(),
                        item.highPrice(),
                        item.lowPrice(),
                        item.volume()
                );
            }

            dailyPriceRepository.save(entity);
        }
    }

    /**
     * 종목 국내/해외 구분, 클라이언트 분기
     */
    private List<ExternalDailyPriceClient.DailyPriceItem> fetchForSecurity(Security security, LocalDate from, LocalDate to) {

        if (security.isKorean()) {
            return kisDailyPriceClient.getDailyPrices(security.getTicker(), from, to);
        } else {
            return finnhubDailyPriceClient.getDailyPrices(security.getTicker(), from, to);
        }
    }
}
