package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.portfolio.domain.Holding;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.dto.PortfolioSummaryResponse;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.stock.dto.StockPriceResponse;
import com.stockmatch.stock.service.StockPriceService;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final StockPriceService stockPriceService;

    /**
     * 사용자에게 포트폴리오가 없으면 생성, dto 반환
     */
    @Transactional
    public PortfolioResponse ensureForUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Portfolio newPortfolio = Portfolio.builder()
                            .user(user)
                            .baseCurrency(Currency.KRW)
                            .build();
                    newPortfolio.setUser(user);

                    try {
                        return portfolioRepository.save(newPortfolio);
                    } catch (DataIntegrityViolationException e) {
                        return portfolioRepository.findByUserId(user.getId())
                                .orElseThrow(() -> e);
                    }
                });

        return new PortfolioResponse(portfolio.getId());
    }

    /**
     * 사용자 포트폴리오 요약 정보 조회
     */
    public PortfolioSummaryResponse getSummaryForUser(Long userId) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 해당 포트폴리오 보유 종목 목록 조회
        List<Holding> holdings = holdingRepository.findByPortfolioId(portfolio.getId());
        List<PortfolioSummaryResponse.HoldingSummary> holdingSummaries = new ArrayList<>();

        BigDecimal totalAsset = BigDecimal.ZERO;        // 총 평가금액 합계
        BigDecimal totalInvested = BigDecimal.ZERO;     // 총 매입금액 합계

        // 각 보유 종목별로 현재가 및 수익률 계산
        for (Holding h : holdings) {
            try {
                StockPriceResponse price;

                // 국내/해외 구분
                if (h.getSecurity().isKorean()) {
                    price = stockPriceService.getKrStockPrice(h.getSecurity().getTicker());
                } else {
                    price = stockPriceService.getUsStockPrice(h.getSecurity().getTicker());
                }

                // 보유수량 및 단가
                BigDecimal currentPrice = BigDecimal.valueOf(price.getCurrentPrice());
                BigDecimal quantity = h.getQuantity();
                BigDecimal avgPrice = h.getAvgPrice();

                // 평가금액 = 현재가 * 수량
                BigDecimal assetValue = currentPrice.multiply(quantity);
                BigDecimal investedValue = avgPrice.multiply(quantity);

                // 수익률 = (현재가 - 평균단가) / 평균단가
                double profitRate = avgPrice.compareTo(BigDecimal.ZERO) == 0
                        ? 0.0
                        : currentPrice.subtract(avgPrice).divide(avgPrice, 6, RoundingMode.HALF_UP).doubleValue();

                totalAsset = totalAsset.add(assetValue);
                totalInvested = totalInvested.add(investedValue);

                // 각 종목별 상세 정보 DTO 변환
                holdingSummaries.add(
                        PortfolioSummaryResponse.HoldingSummary.builder()
                                .ticker(h.getSecurity().getTicker())
                                .name(h.getSecurity().getName())
                                .quantity(quantity.doubleValue())
                                .avgPrice(avgPrice.doubleValue())
                                .currentPrice(currentPrice.doubleValue())
                                .profitRate(profitRate)
                                .build()
                );

            } catch (Exception e) {
                continue;
            }
        }

        // 전체 포트폴리오 수익률 계산
        double totalProfitRate = totalInvested.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : totalAsset.subtract(totalInvested).divide(totalInvested, 6, RoundingMode.HALF_UP).doubleValue();

        // 최종 DTO 반환
        return PortfolioSummaryResponse.builder()
                .totalAsset(totalAsset.doubleValue())
                .totalProfitRate(totalProfitRate)
                .holdings(holdingSummaries)
                .build();
    }
}
