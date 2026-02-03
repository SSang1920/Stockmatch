package com.stockmatch.exchangeRate.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.cache.ExchangeRateCacheService;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import com.stockmatch.exchangeRate.repository.ExchangeRepository;
import com.stockmatch.stock.client.kis.KisUsStockClient;
import com.stockmatch.stock.dto.StockPriceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxRateServiceImpl implements FxRateService {

    private final KisUsStockClient kisUsStockClient;
    private final ExchangeRepository exchangeRepository;
    private final ExchangeService exchangeService;
    private final ExchangeRateCacheService exchangeRateCacheService;

    private static final String USD_KRW_TICKER = "FX@KRW";
    private static final String USD_KRW_NAME = "USD/KRW";

    @Override
    public BigDecimal getUsdToKrwRate(LocalDate date) {
        // 입력값 검증
        if (date == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        ExchangeRate rate = exchangeService.getExchangeRate(
                date,
                FromCurrency.USD,
                ToCurrency.KRW
        );

        return rate.getRate();
    }

    @Override
    public BigDecimal getLatestUsdToKrwRate(LocalDate date) {
        // 입력값 검증
        if (date == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        ExchangeRate rate = exchangeService.getLatestUsdToKrwRate(
                date,
                FromCurrency.USD,
                ToCurrency.KRW
        );

        return rate.getRate();
    }

    // 서버 시작 직후 즉시 실행
    @EventListener(ApplicationReadyEvent.class)
    public void initExchangeRateOnStartUp() {
        fetchAndSaveExchangeRateTrend();
    }

    // 5분마다 환율 정보 수집 및 저장
    @Override
    @Scheduled(cron = "0 0/5 * * * ?")
    public void fetchAndSaveExchangeRateTrend() {
        try {
            log.info("Fetching Exchange Rate for {}", USD_KRW_TICKER);

            StockPriceResponse response = kisUsStockClient.getUsIndexPrice(USD_KRW_TICKER, USD_KRW_NAME);

            if (response.getCurrentPrice() == null) {
                log.warn("Exchange rate is null. Skipping update.");
                return;
            }

            // 최신 환율 가져오기
            BigDecimal currentRate = response.getCurrentPrice();
            LocalDate today = LocalDate.now();

            // DB에 저장
            ExchangeRate exchangeRate = exchangeRepository.findByDateAndFromCurrencyAndToCurrency(
                    today, FromCurrency.USD, ToCurrency.KRW
            ).orElse(null);

            if (exchangeRate == null) {
                exchangeRate = ExchangeRate.builder()
                        .date(today)
                        .fromCurrency(FromCurrency.USD)
                        .toCurrency(ToCurrency.KRW)
                        .rate(currentRate)
                        .build();
                exchangeRepository.save(exchangeRate);
            } else {
                ExchangeRate updatedRate = exchangeRate.toBuilder()
                        .rate(currentRate)
                        .build();
                exchangeRepository.save(updatedRate);
            }

            // Redis에 캐싱
            log.info("Updating Redis Exchange Rate: {}", currentRate);
            exchangeRateCacheService.saveCurrentRate(FromCurrency.USD, ToCurrency.KRW, currentRate);
        } catch (Exception e) {
            log.error("Failed to fetch/save exchange rate", e);
        }
    }
}
