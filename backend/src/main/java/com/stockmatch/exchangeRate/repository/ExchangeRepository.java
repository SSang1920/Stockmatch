package com.stockmatch.exchangeRate.repository;

import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByDateAndFromCurrencyAndToCurrency(
            LocalDate date,
            FromCurrency from,
            ToCurrency to
    );
}
