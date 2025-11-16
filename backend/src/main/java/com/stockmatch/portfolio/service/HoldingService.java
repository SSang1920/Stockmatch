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
import com.stockmatch.stock.client.finnhub.FinnhubClient;
import com.stockmatch.stock.domain.Market;
import com.stockmatch.stock.domain.Security;
import com.stockmatch.stock.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HoldingService {

    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;
    private final HoldingRepository holdingRepository;
    private final FinnhubClient finnhubClient;

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
                .orElseGet(() -> createSecurityOnDemand(request.ticker()));

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

    /**
     * 해외 종목을 Finnhub에서 조회해서 Security 엔티티로 생성/저장
     */
    private Security createSecurityOnDemand(String ticker) {

        // Finnhub에서 종목 조회
        var info = finnhubClient.getUsSymbolProfile(ticker);

        if (info == null || info.name() == null) {
            throw new BusinessException(ErrorCode.SECURITY_NOT_FOUND);
        }

        Security security = Security.builder()
                .ticker(info.ticker() != null ? info.ticker() : ticker)
                .name(info.name())
                .market(Market.USA)
                .currency(Currency.USD)
                .build();

        try {
            return securityRepository.save(security);
        } catch (DataIntegrityViolationException e) {   // 동시성 처리
            return securityRepository.findByTicker(ticker)
                    .orElseThrow(() -> e);
        }
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
