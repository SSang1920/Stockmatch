package com.stockmatch.exchangeRate.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import com.stockmatch.exchangeRate.infra.BokApiClient;
import com.stockmatch.exchangeRate.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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

    /**
     * 특정 날짜의 환율 정보 가져오기
     *
     * @Cacheable 호출될 시, Spring이 Redis 우선 확인
     * - value = "exchange-rates" : Redis에 저장될때 사용되는 그룹 이름
     * - key = "...": 2024-05-21:USD:KRW 와 같이 저장됨
     *
     * key에 해당 하는 데이터가 있으면 캐시 데이터 반환, 없을시 메소드 실행하여 key 형식으로 저장 후 반환
     *
     * @param date 조회할 날짜
     * @param from 기준 통화 (USD)
     * @param to 대상 통화(KRW)
     * @return 조회된 환율 정보
     */
    @Cacheable(value = "exchange-rates", key = "#date.toString()+ ':' + #from.name() + ':' + #to.name()")
    @Transactional
    public ExchangeRate getExchangeRate(LocalDate date, FromCurrency from, ToCurrency to){
        log.info("Cache miss! Finding exchange rate from DB or API for {} from {} to {}", date, from, to);

        // USD -> KRW 환율만 지원 하므로 다른 요청 예외 처리
        if(from != FromCurrency.USD || to != ToCurrency.KRW){
            throw new BusinessException(ErrorCode.UNSUPPORTED_CURRENCY_CONVERSION);
        }
        //DB 확인
        return exchangeRepository.findByDateAndFromCurrencyAndToCurrency(date, from, to)
                .orElseGet(() -> {
                    //DB에 없을시 외부 API를 통해 새로 조회
                    log.info("No exchange rate in DB. Fetching from external API...");
                    BigDecimal rate = bokApiClient.fetchUsdToKrwRate(date);

                    if (rate == null){
                        log.warn("Failed to fetch rate from BOK API for date: {}", date);
                        throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUD);
                    }

                    //DB에 저장, @Cacheable로 Redis에도 자동 저장
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
