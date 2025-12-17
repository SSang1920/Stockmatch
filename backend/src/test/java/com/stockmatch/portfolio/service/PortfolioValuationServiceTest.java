package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.service.FxRateService;
import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.domain.Transaction;
import com.stockmatch.portfolio.dto.PortfolioDailySummaryResponse;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.repository.TransactionRepository;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.dto.DailyPriceResponse;
import com.stockmatch.stock.repository.SecurityRepository;
import com.stockmatch.stock.service.DailyPriceService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortfolioValuationServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private FxRateService fxRateService;
    @Mock
    private DailyPriceService dailyPriceService;

    @InjectMocks
    private PortfolioValuationService portfolioValuationService;

    @Test
    @DisplayName("from > to 이면 INVALID_DATE_RANGE")
    void calculateDailyHistory_invalidRange_throws() {
        // given
        long portfolioId = 1L;

        // when / then
        assertThatThrownBy(() -> portfolioValuationService.calculateDailyHistory(
                portfolioId,
                LocalDate.of(2025, 2, 1),
                LocalDate.of(2025, 1, 1)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INVALID_DATE_RANGE.getMessage());
    }

    @Test
    @DisplayName("포트폴리오 없으면 PORTFOLIO_NOT_FOUND")
    void calculateDailyHistory_portfolioNotFound_throws() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> portfolioValuationService.calculateDailyHistory(
                portfolioId,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 10)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PORTFOLIO_NOT_FOUND.getMessage());
    }

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
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);
        Transaction t1 = mockTransaction(aapl, TradeType.INITIAL_BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(t1));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));       // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"),
                eq(LocalDate.of(2025, 1, 10)),
                eq(LocalDate.of(2025, 1, 10))))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 1, 10)
                );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 1, 10));

        assertThat(result.get(0).totalInvested()).isEqualByComparingTo("1000000.00");
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");
        assertThat(result.get(0).totalPnl()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("전량 매도 후 평가금 0")
    void calculateDailyHistory_sellAllThenZero() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        Transaction sell = mockTransaction(aapl, TradeType.SELL,
                LocalDateTime.of(2025, 1, 11, 9, 0),
                new BigDecimal("10"), new BigDecimal("120"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy, sell));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 11)
                );

        // then
        assertThat(result).hasSize(2);

        // 1/10 : 보유 10주
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");

        // 1/11 : 전량 매도 -> 0
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2025, 1, 11));
        assertThat(result.get(1).totalValue()).isEqualByComparingTo("0.00");
        assertThat(result.get(1).totalInvested()).isEqualByComparingTo("0.00");
        assertThat(result.get(1).totalPnl()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("휴장일이면 전날로 내려가 종가 찾기")
    void calculateDailyHistory_holiday_usesPreviousClose() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        // 1/11 조회는 비어있음(휴일) -> 1/10 조회해서 값 반환
        given(dailyPriceService.getDailyPrices(eq("AAPL"),
                eq(LocalDate.of(2025, 1, 11)),
                eq(LocalDate.of(2025, 1, 11))))
                .willReturn(List.of());

        given(dailyPriceService.getDailyPrices(eq("AAPL"),
                eq(LocalDate.of(2025, 1, 10)),
                eq(LocalDate.of(2025, 1, 10))))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 11),
                        LocalDate.of(2025, 1, 11)
                );

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");
    }

    @Test
    @DisplayName("부분 매도 후 남은 수량만큼만 확인 (BUY 10 -> SELL 3)")
    void calculateDailyHistory_partialSell() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        Transaction sell = mockTransaction(aapl, TradeType.SELL,
                LocalDateTime.of(2025, 1, 11, 9, 0),
                new BigDecimal("3"), new BigDecimal("120"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy, sell));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 11)
                );

        // then
        assertThat(result).hasSize(2);

        // 1/10 : 10주 보유
        assertThat(result.get(0).date()).isEqualTo(LocalDate.of(2025, 1, 10));
        assertThat(result.get(0).totalInvested()).isEqualByComparingTo("1000000.00");
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");

        // 1/11: 7주 보유
        assertThat(result.get(1).date()).isEqualTo(LocalDate.of(2025, 1, 11));
        assertThat(result.get(1).totalInvested()).isEqualByComparingTo("700000.00");
        assertThat(result.get(1).totalValue()).isEqualByComparingTo("770000.00");
        assertThat(result.get(1).totalPnl()).isEqualByComparingTo("70000.00");
    }

    @Test
    @DisplayName("추가 매수하면 평단 갱신 (BUY 10*100 -> BUY 10*200)")
    void calculateDailyHistory_additionalBuy_updateAvgPrice() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy1 = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        Transaction buy2 = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 11, 9, 0),
                new BigDecimal("10"), new BigDecimal("200"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy1, buy2));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 11)
                );

        // then
        assertThat(result).hasSize(2);

        // 1/10 : 10주
        assertThat(result.get(0).totalInvested()).isEqualByComparingTo("1000000.00");
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");

        // 1/11 : 20주
        assertThat(result.get(1).totalInvested()).isEqualByComparingTo("3000000.00");
        assertThat(result.get(1).totalValue()).isEqualByComparingTo("2200000.00");
        assertThat(result.get(1).totalPnl()).isEqualByComparingTo("-800000.00");
    }

    @Test
    @DisplayName("전량 매도 후 재진입하면 중간 기간은 0, 재진입 이후만 평가")
    void calculateDailyHistory_reentry_afterSelAll() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        Transaction sellAll = mockTransaction(aapl, TradeType.SELL,
                LocalDateTime.of(2025, 1, 11, 9, 0),
                new BigDecimal("10"), new BigDecimal("120"));

        Transaction reBuy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 12, 9, 0),
                new BigDecimal("5"), new BigDecimal("200"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy, sellAll, reBuy));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 12)
                );

        // then
        assertThat(result).hasSize(3);

        // 1/10 : 10주
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1100000.00");

        // 1/11 : 0주 전량매도
        assertThat(result.get(1).totalValue()).isEqualByComparingTo("0.00");
        assertThat(result.get(1).totalInvested()).isEqualByComparingTo("0.00");

        // 1/12 : 5주 재진입
        assertThat(result.get(2).totalValue()).isEqualByComparingTo("550000.00");
        assertThat(result.get(2).totalInvested()).isEqualByComparingTo("1000000.00");
    }

    @Test
    @DisplayName("종목 2개(KR + US) 합산 계산")
    void calculateDailyHistory_twoSecurities_sumUp() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);
        Security samsung = mockSecurity(5000L, "005930", true);

        Transaction buyAapl = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        Transaction buySamsung = mockTransaction(samsung, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("2"), new BigDecimal("50000"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buyAapl, buySamsung));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl, samsung));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));    // 종가 고정

        given(dailyPriceService.getDailyPrices(eq("005930"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("60000"))));  // 종가 고정

        // when
        List<PortfolioDailySummaryResponse> result =
                portfolioValuationService.calculateDailyHistory(
                        portfolioId,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 10)
                );

        // then
        assertThat(result).hasSize(1);

        // AAPL : invested = 1,000,00 / value = 1,110,000
        // 삼성 : invested = 100,000 / value = 120,000
        assertThat(result.get(0).totalInvested()).isEqualByComparingTo("1100000.00");
        assertThat(result.get(0).totalValue()).isEqualByComparingTo("1220000.00");
        assertThat(result.get(0).totalPnl()).isEqualByComparingTo("120000");
    }

    @Test
    @DisplayName("US 종목이 여러 개여도 환율 조회는 하루 1번만 조회")
    void calculateDailyHistory_fxRateCalledOncePerDay() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);
        Security msft = mockSecurity(4295L, "MSFT", false);

        Transaction buyAapl = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("1"), new BigDecimal("100"));

        Transaction buyMsft = mockTransaction(msft, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("1"), new BigDecimal("100"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buyAapl, buyMsft));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl, msft));

        given(fxRateService.getLatestUsdToKrwRate(any(LocalDate.class)))
                .willReturn(new BigDecimal("1000"));    // 환율 고정

        given(dailyPriceService.getDailyPrices(anyString(), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of(dailyPrice(new BigDecimal("110"))));

        LocalDate date = LocalDate.of(2025, 1, 10);

        // when
        portfolioValuationService.calculateDailyHistory(portfolioId, date, date);

        // then
        verify(fxRateService, times(1)).getLatestUsdToKrwRate(date);
    }

    @Test
    @DisplayName("룩백 기간 내 종가가 없으면 DAILY_PRICE_NOT_FOUND")
    void calculateDailyHistory_noPriceWithinLookback_throws() {
        // given
        long portfolioId = 1L;
        given(portfolioRepository.existsById(portfolioId)).willReturn(true);

        Security aapl = mockSecurity(4294L, "AAPL", false);

        Transaction buy = mockTransaction(aapl, TradeType.BUY,
                LocalDateTime.of(2025, 1, 10, 9, 0),
                new BigDecimal("10"), new BigDecimal("100"));

        given(transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolioId))
                .willReturn(List.of(buy));

        given(securityRepository.findAllById(anyIterable()))
                .willReturn(List.of(aapl));

        given(dailyPriceService.getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of());     // 빈 리스트

        // when
        Throwable thrown = catchThrowable(() -> portfolioValuationService.calculateDailyHistory(
                portfolioId,
                LocalDate.of(2025, 1, 11),
                LocalDate.of(2025, 1, 11)
        ));

        // then
        assertThat(thrown).isInstanceOf(BusinessException.class);
        BusinessException businessException = (BusinessException) thrown;
        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.DAILY_PRICE_NOT_FOUND);

        verify(dailyPriceService, atLeastOnce())
                .getDailyPrices(eq("AAPL"), any(LocalDate.class), any(LocalDate.class));
    }

    // ===== 헬퍼 메서드 =====
    private Security mockSecurity(long id, String ticker, boolean isKorean) {

        Security security = mock(Security.class);
        lenient().when(security.getId()).thenReturn(id);
        lenient().when(security.getTicker()).thenReturn(ticker);
        lenient().when(security.isKorean()).thenReturn(isKorean);
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

    private DailyPriceResponse dailyPrice(BigDecimal p) {
        return new DailyPriceResponse(
                LocalDate.of(2025, 1, 10),
                p,
                p,
                p,
                p,
                p
        );
    }
}
