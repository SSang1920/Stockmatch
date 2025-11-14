package com.stockmatch.portfolio.service;

import com.stockmatch.common.exception.BusinessException;
import com.stockmatch.common.exception.ErrorCode;
import com.stockmatch.portfolio.domain.Currency;
import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.dto.PortfolioResponse;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.user.member.domain.User;
import com.stockmatch.user.member.repository.UserRepository;
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
}
