package com.stockmatch.exchangeRate.controller;


import com.stockmatch.common.api.ApiResponse;
import com.stockmatch.exchangeRate.domain.ExchangeRate;
import com.stockmatch.exchangeRate.domain.FromCurrency;
import com.stockmatch.exchangeRate.domain.ToCurrency;
import com.stockmatch.exchangeRate.service.ExchangeService;
import com.stockmatch.exchangeRate.service.FxRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/exchange-rate")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeService exchangeService;
    private final FxRateService fxRateService;

    /**
     * 특정 날짜의 환율 정보 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ExchangeRate>> getRate(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date,
            @RequestParam("from") FromCurrency from,
            @RequestParam("to") ToCurrency to){

        ExchangeRate data = exchangeService.getExchangeRate(date, from, to);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * 오늘 날짜 기준으로 redis에서 최신 환율 조회
     */
    @GetMapping("/usd-krw")
    public ResponseEntity<ApiResponse<BigDecimal>> getUsdToKrwRate() {
        BigDecimal rate = fxRateService.getLatestUsdToKrwRate(LocalDate.now());
        return ResponseEntity.ok(ApiResponse.ok(rate));
    }
}
