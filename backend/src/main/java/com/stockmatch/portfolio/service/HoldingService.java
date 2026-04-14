package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.portfolio.domain.Holding;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.dto.HoldingRequest;
import com.stockmatch.portfolio.dto.HoldingResponse;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionService transactionService;

    /**
     * 로그인한 사용자의 포트폴리오에 보유 종목 1개 추가
     */
    @Transactional
    public HoldingResponse addHolding(Long userId, HoldingRequest request) {
        // 수량 검증
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 종목 조회
        Security security = securityRepository.findByTicker(request.ticker())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 기존 보유종목 여부 확인
        Holding holding = holdingRepository.findByPortfolioIdAndSecurityId(portfolio.getId(), security.getId()).orElse(null);

        Currency securityCurrency = security.getCurrencyCode();

        boolean isNewHolding = (holding == null);

        if (isNewHolding) {
            holding = Holding.builder()
                    .portfolio(portfolio)
                    .security(security)
                    .quantity(request.quantity())
                    .avgPrice(request.avgPrice())
                    .currency(securityCurrency)
                    .build();
        } else {
            // 추가 매수 시 평단가 및 수량 재계산
            BigDecimal existingQty = holding.getQuantity();
            BigDecimal existingAvg = holding.getAvgPrice();
            BigDecimal newQty = request.quantity();
            BigDecimal newPrice = request.avgPrice();

            BigDecimal totalQty = existingQty.add(newQty);
            BigDecimal totalCost = (existingAvg.multiply(existingQty)).add(newPrice.multiply(newQty));
            BigDecimal newAvg = totalCost.divide(totalQty, 4, RoundingMode.HALF_UP);

            holding.updateQuantityAndAvgPrice(totalQty, newAvg);
            holding.updateCurrency(securityCurrency);
        }

        holdingRepository.save(holding);

        // 신규 보유라면 INITIAL_BUY 거래 기록
        if (isNewHolding) {
            transactionService.recordInitialBuy(
                    portfolio,
                    security,
                    request.quantity(),
                    request.avgPrice(),
                    LocalDateTime.now()
            );
        }

        // 응답 DTO 변환
        return HoldingResponse.builder()
                .id(holding.getId())
                .ticker(security.getTicker())
                .name(security.getName())
                .quantity(holding.getQuantity())
                .avgPrice(holding.getAvgPrice())
                .currency(holding.getCurrency().name())
                .build();
    }

    /**
     * 로그인한 사용자의 포트폴리오에 보유 종목 1개 수정
     */
    @Transactional
    public HoldingResponse updateHolding(Long userId, Long holdingId, HoldingRequest request) {
        // 수량 검증
        if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 종목 조회
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));

        // 내 포트폴리오의 종목이 맞는지 검증
        if (!holding.getPortfolio().getId().equals(portfolio.getId())) {
            throw new BusinessException(ErrorCode.HOLDING_NOT_IN_PORTFOLIO);
        }

        // 업데이트
        holding.updateQuantityAndAvgPrice(request.quantity(), request.avgPrice());

        return HoldingResponse.builder()
                .id(holding.getId())
                .ticker(holding.getSecurity().getTicker())
                .name(holding.getSecurity().getName())
                .quantity(holding.getQuantity())
                .avgPrice(holding.getAvgPrice())
                .currency(holding.getCurrency().name())
                .build();
    }

    /**
     * 내 보유 종목 조회
     */
    public List<HoldingResponse> getMyHoldings(Long userId) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 보유종목 조회
        List<Holding> holdings = holdingRepository.findAllWithSecurityByPortfolioId(portfolio.getId());

        return holdings.stream()
                .map(h -> HoldingResponse.builder()
                        .id(h.getId())
                        .ticker(h.getSecurity().getTicker())
                        .name(h.getSecurity().getName())
                        .quantity(h.getQuantity())
                        .avgPrice(h.getAvgPrice())
                        .currency(h.getCurrency().name())
                        .build()
                ).toList();
    }

    /**
     * 내 보유 종목 삭제
     */
    @Transactional
    public void deleteMyHolding(Long userId, Long holdingId) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 보유종목 조회
        Holding holding = holdingRepository.findById(holdingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLDING_NOT_FOUND));

        // 이 보유종목이 내 포트폴리오 것인지 검증
        if (!holding.getPortfolio().getId().equals(portfolio.getId())) {
            throw new BusinessException(ErrorCode.HOLDING_NOT_IN_PORTFOLIO);
        }

        holdingRepository.delete(holding);
    }
}
