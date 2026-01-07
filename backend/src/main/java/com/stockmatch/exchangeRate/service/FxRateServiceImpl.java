package com.stockmatch.exchangeRate.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FxRateServiceImpl implements FxRateService {

    private final ExchangeService exchangeService;

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
}
