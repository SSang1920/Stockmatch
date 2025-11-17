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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 특정 포트폴리오의 거래 내역 전체 조회
     */
    public List<TransactionResponse> getTransactions(Long userId, Long portfolioId) {

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        return transactionRepository
                .findByPortfolioIdOrderByTradeAtDesc(portfolio.getId())
                .stream()
                .map(TransactionResponse::from)
                .toList();
    }

    /**
     * 매수 기록
     */
    @Transactional
    public TransactionResponse buy(Long userId, Long portfolioId, TransactionCreateRequest request) {

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        // 종목 조회
        Security security = securityRepository.findById(request.SecurityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 거래기록 dto로 변환해서 저장
        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .security(security)
                .type(TradeType.BUY)
                .quantity(request.quantity())
                .price(request.price())
                .fee(request.fee())
                .tradeAt(request.TradeAt())
                .memo(request.memo())
                .build();
        transactionRepository.save(transaction);

        // 보유종목 업데이트
        Holding holding = getOrCreateHolding(portfolio, security);
        updateHoldingOnBuy(holding, request);

        return TransactionResponse.from(transaction);
    }

    /**
     * 매도 기록
     */
    @Transactional
    public TransactionResponse sell(Long userId, Long portfolioId, TransactionCreateRequest request) {

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        // 종목 조회
        Security security = securityRepository.findById(request.SecurityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 보유종목 조회
        Holding holding = holdingRepository.findByPortfolioAndSecurity(portfolio, security)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));

        // 수량 검증
        if (holding.getQuantity().compareTo(request.quantity()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        // 거래기록 dto로 변환해서 저장
        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .security(security)
                .type(TradeType.SELL)
                .quantity(request.quantity())
                .price(request.price())
                .fee(request.fee())
                .tradeAt(request.TradeAt())
                .memo(request.memo())
                .build();
        transactionRepository.save(transaction);

        // 보유 종목 수량 감소
        updateHoldingOnSell(holding, request);

        return TransactionResponse.from(transaction);
    }

    /**
     * 포트폴리오가 해당 사용자 소유인지 검증 후 반환 메서드
     */
    private Portfolio findPortfolioOfUser(Long userId, Long portfolioId) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 포트폴리오 사용자 검증
        if (!portfolio.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        return portfolio;
    }

    /**
     * 수수료가 null이면 0으로 대체
     */
    private BigDecimal defaultFee(BigDecimal fee) {
        return fee != null ? fee : BigDecimal.ZERO;
    }

    /**
     * 보유종목이 없으면 새로 만들고, 있으면 그대로 반환
     */
    private Holding getOrCreateHolding(Portfolio portfolio, Security security) {
        // 보유종목 조회
        return holdingRepository.findByPortfolioAndSecurity(portfolio, security)
                .orElseGet(() -> {
                    Holding newHolding = Holding.builder()
                            .portfolio(portfolio)
                            .security(security)
                            .quantity(BigDecimal.ZERO)
                            .avgPrice(BigDecimal.ZERO)
                            .build();
                    return holdingRepository.save(newHolding);
                });
    }

    /**
     * 매수 시 보유종목 수량 및 평균단가 갱신
     */
    private void updateHoldingOnBuy(Holding holding, TransactionCreateRequest request) {
        BigDecimal oldQuantity = holding.getQuantity();
        BigDecimal buyQuantity = request.quantity();

        // 수수료 포함 총 매수금 계산
        BigDecimal oldTotal = holding.getAvgPrice().multiply(oldQuantity);
        BigDecimal newTotal = oldTotal.add(request.price().multiply(buyQuantity)).add(defaultFee(request.fee()));

        BigDecimal newQuantity = oldQuantity.add(buyQuantity);

        BigDecimal newAvg = newQuantity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : newTotal.divide(newQuantity, 4, RoundingMode.HALF_UP);

        // 수량 및 평균단가 갱신
        holding.updateQuantity(newQuantity);
        holding.updateAvgPrice(newAvg);
    }

    /**
     * 매도 시  보유 수량 감소, 수량 0이면 보유종목 삭제
     */
    private void updateHoldingOnSell(Holding holding, TransactionCreateRequest request) {
        BigDecimal oldQuantity = holding.getQuantity();
        BigDecimal sellQuantity = request.quantity();

        BigDecimal newQuantity = oldQuantity.subtract(sellQuantity);

        if (newQuantity.compareTo(BigDecimal.ZERO) == 0) {
            holdingRepository.delete(holding);
            return;
        }

        // 수량 갱신
        holding.updateQuantity(newQuantity);
    }
}
