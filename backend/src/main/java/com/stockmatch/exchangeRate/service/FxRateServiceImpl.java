package com.stockmatch.exchangeRate.service;

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
        ExchangeRate rate = exchangeService.getExchangeRate(
                date,
                FromCurrency.USD,
                ToCurrency.KRW
        );

        return rate.getRate();
    }

    @Override
    public BigDecimal getLatestUsdToKrwRate(LocalDate date) {
        ExchangeRate rate = exchangeService.getLatestUsdToKrwRate(
                date,
                FromCurrency.USD,
                ToCurrency.KRW
        );

        return rate.getRate();
    }
}
