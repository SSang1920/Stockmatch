package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.exchangeRate.service.FxRateService;
import com.stockmatch.portfolio.domain.*;
import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
import com.stockmatch.portfolio.dto.TransactionCreateRequest;
import com.stockmatch.portfolio.dto.TransactionResponse;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioDailySummaryRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.repository.TransactionRepository;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;
    private final PortfolioDailySummaryRepository dailySummaryRepository;
    private final PortfolioValuationService valuationService;
    private final FxRateService fxRateService;

    /**
     * 초기 보유분을 INITIAL_BUY로 기록
     */
    @Transactional
    public Transaction recordInitialBuy(Portfolio portfolio, Security security, BigDecimal quantity, BigDecimal avgPrice, LocalDateTime tradeDate) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .security(security)
                .type(TradeType.INITIAL_BUY)
                .quantity(quantity)
                .price(avgPrice)
                .fee(BigDecimal.ZERO)
                .tradeAt(tradeDate)
                .memo("초기 보유분 등록")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);

        // 초기 보유분 등록 후 스냅샷 동기화
        syncSnapshot(portfolio);

        return savedTx;
    }

    /**
     * 특정 포트폴리오의 거래 내역 전체 조회
     */
    public Slice<TransactionResponse> getTransactions(Long userId, Long portfolioId, Pageable pageable) {

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        return transactionRepository
                .findByPortfolioIdOrderByTradeAtDesc(portfolio.getId(), pageable)
                .map(TransactionResponse::from);
    }

    /**
     * 매수 기록
     */
    @Transactional
    public TransactionResponse buy(Long userId, Long portfolioId, TransactionCreateRequest request) {
        // 수량, 가격 검증
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        // 종목 조회
        Security security = securityRepository.findById(request.securityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 거래기록 dto로 변환해서 저장
        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .security(security)
                .type(TradeType.BUY)
                .quantity(request.quantity())
                .price(request.price())
                .fee(request.fee())
                .tradeAt(request.tradeAt())
                .memo(request.memo())
                .build();
        transactionRepository.save(transaction);

        // 보유종목 업데이트
        Holding holding = getOrCreateHolding(portfolio, security);
        updateHoldingOnBuy(holding, request);

        // 매수 후 스냅샷 동가화
        syncSnapshot(portfolio);

        return TransactionResponse.from(transaction);
    }

    /**
     * 매도 기록
     */
    @Transactional
    public TransactionResponse sell(Long userId, Long portfolioId, TransactionCreateRequest request) {
        // 검증 (가격, 수량)
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0 ||
                request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 포트폴리오 조회
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        // 종목 조회
        Security security = securityRepository.findById(request.securityId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 보유종목 조회
        Holding holding = holdingRepository.findByPortfolioAndSecurity(portfolio, security)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));

        // 수량 검증
        if (holding.getQuantity().compareTo(request.quantity()) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_HOLDING_QUANTITY);
        }

        // 해당 종목의 통화 기준 수익
        BigDecimal profitInOriginalCurrency = request.price()
                .subtract(holding.getAvgPrice())
                .multiply(request.quantity());

        BigDecimal finalProfitKrw;

        if (!security.isKorean()) {
            BigDecimal usdToKrw = fxRateService.getLatestUsdToKrwRate(LocalDate.now());
            if (usdToKrw == null) {
                throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
            }
            finalProfitKrw = profitInOriginalCurrency.multiply(usdToKrw);
        } else {
            finalProfitKrw = profitInOriginalCurrency;
        }

        portfolio.updateRealizedPnl(finalProfitKrw.setScale(2, RoundingMode.HALF_UP));

        // 거래기록 dto로 변환해서 저장
        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .security(security)
                .type(TradeType.SELL)
                .quantity(request.quantity())
                .price(request.price())
                .fee(request.fee())
                .tradeAt(request.tradeAt())
                .memo(request.memo())
                .build();
        transactionRepository.save(transaction);

        // 보유 종목 수량 감소
        updateHoldingOnSell(holding, request);

        // 매도 후 스냅샷 동기화
        syncSnapshot(portfolio);

        return TransactionResponse.from(transaction);
    }

    /**
     * 특정 거래 내역 삭제 및 보유 종목 재계산
     */
    @Transactional
    public void deleteTransaction(Long userId, Long portfolioId, Long transactionId) {
        Portfolio portfolio = findPortfolioOfUser(userId, portfolioId);

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSACTION_NOT_FOUND));

        // 해당 거래가 이 포트폴리오의 거래가 맞는지 검증
        if (!transaction.getPortfolio().getId().equals(portfolioId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Security security = transaction.getSecurity();

        // 거래 내역 삭제
        transactionRepository.delete(transaction);
        transactionRepository.flush();

        // 보유 종목 재계산
        recalculateHolding(portfolio, security);

        // 실현 손익 전체 재계산
        recalculateTotalRealizedPnl(portfolio);

        // 삭제 후 스냅샷 동기화
        syncSnapshot(portfolio);
    }

    /**
     * 포트폴리오가 해당 사용자 소유인지 검증 후 반환 메서드
     */
    private Portfolio findPortfolioOfUser(Long userId, Long portfolioId) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
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

    /**
     * 보유 종목 재계산 및 평단가 재계산
     */
    private void recalculateHolding(Portfolio portfolio, Security security) {
        List<Transaction> remainingTxs = transactionRepository.findByPortfolioAndSecurityOrderByTradeAtAsc(portfolio, security);

        Holding holding = holdingRepository.findByPortfolioAndSecurity(portfolio, security).orElse(null);

        // 남은 거래 내역이 없으면 보유 종목에서 삭제
        if (remainingTxs.isEmpty()) {
            if (holding != null) holdingRepository.delete(holding);
            return;
        }

        // 처음부터 계산
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (Transaction tx : remainingTxs) {
            if (tx.getType() == TradeType.BUY || tx.getType() == TradeType.INITIAL_BUY) {
                totalQuantity = totalQuantity.add(tx.getQuantity());
                totalCost = totalCost.add(tx.getPrice().multiply(tx.getQuantity())).add(defaultFee(tx.getFee()));
            } else if (tx.getType() == TradeType.SELL) {
                if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentAvg = totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP);
                    totalQuantity = totalQuantity.subtract(tx.getQuantity());
                    totalCost = totalQuantity.multiply(currentAvg);
                }
            }
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            if (holding != null) holdingRepository.delete(holding);
        } else {
            if (holding == null) {
                holding = getOrCreateHolding(portfolio, security);
            }

            BigDecimal finalAvgPrice = totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP);
            holding.updateQuantity(totalQuantity);
            holding.updateAvgPrice(finalAvgPrice);
        }
    }

    /**
     * 오늘자 스냅샷을 최신 상태로 유지
     */
    private void syncSnapshot(Portfolio portfolio) {
        LocalDate today = LocalDate.now();

        // 현재 실시간 평가액 계산
        PortfolioValuationResponse current = valuationService.calculate(portfolio.getId());

        // 오늘 이미 기록된 스냅샷이 있는지 조회
        PortfolioDailySummary summary = dailySummaryRepository
                .findByPortfolioIdAndDate(portfolio.getId(), today)
                .orElseGet(() -> PortfolioDailySummary.builder()
                        .portfolio(portfolio)
                        .date(today)
                        .build());

        summary.updateSnapshotValues(
                current.totalInvested(),
                current.totalValue(),
                current.totalPnlAmount(),
                current.totalPnlRate(),
                portfolio.getRealizedPnl() // 실현 손익 포함
        );

        dailySummaryRepository.save(summary);
    }

    /**
     * 포트폴리오의 모든 거래를 조회하여 누적 실현 손익 계산
     */
    private void recalculateTotalRealizedPnl(Portfolio portfolio) {
        List<Transaction> allTxs = transactionRepository.findAllByPortfolioIdOrderByTradeAtAsc(portfolio.getId());

        BigDecimal newRealizedPnl = BigDecimal.ZERO;
        HashMap<Long, BigDecimal> avgPriceMap = new HashMap<>();
        HashMap<Long, BigDecimal> qtyMap = new HashMap<>();

        for (Transaction tx : allTxs) {
            Long sId = tx.getSecurity().getId();
            BigDecimal currentQty = qtyMap.getOrDefault(sId, BigDecimal.ZERO);
            BigDecimal currentAvg = avgPriceMap.getOrDefault(sId, BigDecimal.ZERO);

            if (tx.getType() == TradeType.BUY || tx.getType() == TradeType.INITIAL_BUY) {
                BigDecimal oldTotal = currentAvg.multiply(currentQty);
                BigDecimal newTotal = oldTotal.add(tx.getPrice().multiply(tx.getQuantity())).add(defaultFee(tx.getFee()));
                BigDecimal newQty = currentQty.add(tx.getQuantity());

                qtyMap.put(sId, newQty);
                avgPriceMap.put(sId, newQty.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : newTotal.divide(newQty, 4, RoundingMode.HALF_UP));
            } else if (tx.getType() == TradeType.SELL) {
                BigDecimal profit = tx.getPrice().subtract(currentAvg).multiply(tx.getQuantity());
                newRealizedPnl = newRealizedPnl.add(profit);

                qtyMap.put(sId, currentQty.subtract(tx.getQuantity()));
            }
        }

        portfolio.setRealizedPnl(newRealizedPnl.setScale(2, RoundingMode.HALF_UP));

    }

}
