package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.dto.PortfolioSummaryResponse;
import com.stockmatch.portfolio.repository.HoldingRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.repository.TransactionRepository;
import com.stockmatch.user.domain.User;
import com.stockmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    /**
     * 사용자에게 포트폴리오가 없으면 생성, dto 반환
     * @param userId 포트폴리오 소유할 사용자 ID
     * @return 포트폴리오 ID
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
     * 포트폴리오 대시보드 요약 정보 반환
     * @param userId 사용자 ID
     * @return 대시보드 요약 정보
     */
    public PortfolioSummaryResponse getSummaryForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Portfolio portfolio = portfolioRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        final long holdingCnt = holdingRepository.countByPortfolioId(portfolio.getId());
        final long transactionCnt = transactionRepository.countByPortfolioId(portfolio.getId());

        return new PortfolioSummaryResponse(
                portfolio.getId(),
                user.getId(),
                portfolio.getBaseCurrency().name(),
                Math.toIntExact(holdingCnt),
                Math.toIntExact(transactionCnt)
        );
    }
}
