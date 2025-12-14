package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.service.FxRateService;
import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.domain.Transaction;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.repository.TransactionRepository;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.DailyPriceResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import com.stockmatch.stock.service.DailyPriceService;
import com.stockmatch.stock.service.StockPriceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class PortfolioValuationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private HoldingRepository holdingRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StockPriceService stockPriceService;
    @Mock
    private FxRateService fxRateService;
    @Mock
    private DailyPriceService dailyPriceService;

    @InjectMocks
    private PortfolioValuationService portfolioValuationService;

    @Test
    @DisplayName("거래내역이 없으면 TRANSACTION_NOT_FOUND 예외")
    void calculateDailyHistory_noTransactions_throws() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of());

        // when / then
        assertThatThrownBy(() -> portfolioValuationService.calculateDailyHistory(
                1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.TRANSACTION_NOT_FOUND.message());
    }

    @Test
    @DisplayName("from이 첫 거래일 이전이면 첫 거래일부터만 결과 만들기")
    void calculateDailyHistory_adjustStartDateToFirstTrade() {
        // given
        long portfolioId = 1L;

        Security aapl = mockSecurity(4294L, "AAPL", false);
        Transaction t1 = mockTransaction(aapl, TradeType.INITIAL_BUY, LocalDateTime.of(2025, 1, 10, 9, 0), new BigDecimal("10"), new BigDecimal("100"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(t1));

        given(securityRepository.findById(4294L))
                .willReturn(Optional.of(aapl));

        // 환율
        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));
    }

    // ===== 헬퍼 메서드 =====
    private Security mockSecurity(long id, String ticker, boolean isKorean) {
        Security security = mock(Security.class);
        given(security.getId()).willReturn(id);
        given(security.getTicker()).willReturn(ticker);
        given(security.isKorean()).willReturn(isKorean);
        return security;
    }

    private Transaction mockTransaction(Security security, TradeType type, LocalDateTime tradeAt, BigDecimal quantity, BigDecimal price) {
        Transaction transaction = mock(Transaction.class);
        given(transaction.getSecurity()).willReturn(security);
        given(transaction.getType()).willReturn(type);
        given(transaction.getTradeAt()).willReturn(tradeAt);
        given(transaction.getQuantity()).willReturn(quantity);
        given(transaction.getPrice()).willReturn(price);
        return transaction;
    }

    private DailyPriceResponse mockClose(BigDecimal close) {
        DailyPriceResponse response = mock(DailyPriceResponse.class);
        given(response.closePrice()).willReturn(close);
        return response;
    }
}
