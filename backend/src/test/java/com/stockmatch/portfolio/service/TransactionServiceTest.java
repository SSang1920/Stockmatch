package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Holding;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.domain.TradeType;
import com.stockmatch.portfolio.domain.Transaction;
import com.stockmatch.portfolio.dto.TransactionCreateRequest;
import com.stockmatch.portfolio.dto.TransactionResponse;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.repository.TransactionRepository;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import com.stockmatch.user.member.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("매수 성공: 거래 저장 + 보유수량/평단 업데이트")
    void buy_success() {

        // Given
        Long userId = 1L;
        Long portfolioId = 10L;
        Long securityId = 100L;

        // 유저/포트폴리오/종목/보유 정보 준비
        User user = User.builder()
                .id(userId)
                .build();

        Portfolio portfolio = Portfolio.builder()
                .id(portfolioId)
                .user(user)
                .build();

        Security security = Security.builder()
                .id(securityId)
                .build();

        Holding holding = Holding.builder()
                .portfolio(portfolio)
                .security(security)
                .quantity(new BigDecimal("5"))
                .avgPrice(new BigDecimal("10000"))
                .build();

        given(portfolioRepository.findById(portfolioId))
                .willReturn(Optional.of(portfolio));
        given(securityRepository.findById(securityId))
                .willReturn(Optional.of(security));
        given(holdingRepository.findByPortfolioAndSecurity(portfolio, security))
                .willReturn(Optional.of(holding));
        given(transactionRepository.save(any(Transaction.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        TransactionCreateRequest request = new TransactionCreateRequest(
                securityId,
                new BigDecimal("3"),
                new BigDecimal("12000"),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                "테스트 매수"
        );

        // When
        TransactionResponse buy = transactionService.buy(userId, portfolioId, request);

        // Then
        ArgumentCaptor<Transaction> transactionArgumentCaptor =
                ArgumentCaptor.forClass(Transaction.class);
        then(transactionRepository)
                .should()
                .save(transactionArgumentCaptor.capture());

        Transaction saved = transactionArgumentCaptor.getValue();
        assertThat(saved.getType()).isEqualTo(TradeType.BUY);
        assertThat(saved.getQuantity()).isEqualByComparingTo("3");      // 매수 수량 검증
        assertThat(saved.getPrice()).isEqualByComparingTo("12000");     // 매수 평단가 검증

        assertThat(holding.getQuantity()).isEqualByComparingTo("8");    // 보유종목 수량 검증: 5 + 3 = 8
        assertThat(holding.getAvgPrice()).isEqualByComparingTo("10750"); // 보유종목 평단가 검증: (10000 * 5) + (12000 * 3) / 8 = 10750
    }

    @Test
    @DisplayName("매도 실패: 보유 수량보다 많이 팔면 예외 발생")
    void sell_insufficientQuantity() {

        // given
        Long userId = 1L;
        Long portfolioId = 10L;
        Long securityId = 100L;

        // 유저/포트폴리오/종목/보유 정보 준비
        User user = User.builder()
                .id(userId)
                .build();

        Portfolio portfolio = Portfolio.builder()
                .id(portfolioId)
                .user(user)
                .build();

        Security security = Security.builder()
                .id(securityId)
                .build();

        Holding holding = Holding.builder()
                .portfolio(portfolio)
                .security(security)
                .quantity(new BigDecimal("5"))
                .avgPrice(new BigDecimal("10000"))
                .build();

        given(portfolioRepository.findById(portfolioId))
                .willReturn(Optional.of(portfolio));
        given(securityRepository.findById(securityId))
                .willReturn(Optional.of(security));
        given(holdingRepository.findByPortfolioAndSecurity(portfolio, security))
                .willReturn(Optional.of(holding));

        TransactionCreateRequest request = new TransactionCreateRequest(
                securityId,
                new BigDecimal("10"),
                new BigDecimal("11000"),
                BigDecimal.ZERO,
                LocalDateTime.now(),
                "과매도 테스트"
        );

        // when & then
        assertThatThrownBy(() -> transactionService.sell(userId, portfolioId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_HOLDING_QUANTITY.message());
    }
}
