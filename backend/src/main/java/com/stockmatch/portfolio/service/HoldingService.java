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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;

    /**
     * 로그인한 사용자의 포트폴리오에 보유 종목 1개 추가
     */
    @Transactional
    public HoldingResponse addOrUpdateHolding(Long userId, HoldingRequest request) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 종목 조회
        Security security = securityRepository.findByTicker(request.ticker())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // 기존 보유종목 여부 확인
        Holding holding = holdingRepository.findByPortfolioIdAndSecurityId(portfolio.getId(), security.getId()).orElse(null);

        if (holding == null) {
            holding = new Holding(
                    null,
                    portfolio,
                    security,
                    request.quantity(),
                    request.avgPrice(),
                    security.getCurrency() != null ? security.getCurrency() : Currency.KRW
            );
        } else {
            // 있으면 정보 수정
            holding.updateQuantityAndAvgPrice(request.quantity(), request.avgPrice());
        }

        holdingRepository.save(holding);

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
}
