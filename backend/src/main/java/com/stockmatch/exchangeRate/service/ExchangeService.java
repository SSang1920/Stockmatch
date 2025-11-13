package com.stockmatch.exchangeRate.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.cache.ExchangeRateCacheService;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import com.stockmatch.exchangeRate.infra.BokApiClient;
import com.stockmatch.exchangeRate.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final BokApiClient bokApiClient;
    private final ExchangeRateCacheService cacheService;

    public ExchangeRate getExchangeRate(LocalDate date, FromCurrency from, ToCurrency to) {
        // 데이터가 없으면 fetchFromDbOrApi를 실행하여 결과를 캐시에 저장
        return cacheService.getOrLoadExchangeRate(date, from, to, () ->{
            //캐시에 데이터가 없을때 실행
            log.info("Executing loader for Exchange Rate. Date: {}, From: {}, To: {}", date, from, to);
            return fetchFromDbOrApi(date, from, to);
        });
    }

    @Transactional
    private ExchangeRate fetchFromDbOrApi(LocalDate date, FromCurrency from, ToCurrency to){
        if (from != FromCurrency.USD || to != ToCurrency.KRW) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_CURRENCY_CONVERSION);
        }
        // DB 확인
        return exchangeRepository.findByDateAndFromCurrencyAndToCurrency(date, from, to)
                .orElseGet(() -> {
                    //  DB에 없으면, 외부 API 호출
                    log.info("No exchange rate in DB. Fetching from external API...");
                    BigDecimal rate = bokApiClient.fetchUsdToKrwRate(date);

                    if (rate == null) {
                        log.warn("Failed to fetch rate from BOK API for date: {}", date);
                        throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
                    }

                    // DB에 저장
                    ExchangeRate newExchangeRate = ExchangeRate.builder()
                            .date(date)
                            .fromCurrency(from)
                            .toCurrency(to)
                            .rate(rate)
                            .build();
                    return exchangeRepository.save(newExchangeRate);
                });
    }

    /**
     * 매일 오전 11시 30분 환율 정보를 조회하여 캐시에 저장
     */
    @Scheduled(cron = "0 30 11 * * MON-FRI",zone =  "Asia/Seoul")
    public void scheduleDailyExchangeRateUpdate() {
        LocalDate today = LocalDate.now();
        log.info("scheduled task: Warming up daily exchange rate cache for {}", today);
        try{
            getExchangeRate(today, FromCurrency.USD, ToCurrency.KRW);
        } catch (Exception e){
            log.error("Scheduled exchange rate update failed", e);
        }
    }
}
