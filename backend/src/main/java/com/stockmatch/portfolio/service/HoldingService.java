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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class HoldingService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;

    public HoldingResponse addHolding(Long userId, HoldingRequest request) {

        // 포트폴리오 조회
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 종목 조회
        Security security = securityRepository.findByTicker(request.getTicker())
                .orElseThrow(() -> new BusinessException(ErrorCode.SECURITY_NOT_FOUND));

        // Holding 생성 및 저장
        Holding holding = new Holding(
                null,
                portfolio,
                security,
                request.getQuantity(),
                request.getAvgPrice(),
                security.getCurrency() != null ? security.getCurrency() : Currency.KRW
        );
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
