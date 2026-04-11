package com.stockmatch.portfolio.scheduler;

import com.stockmatch.portfolio.domain.Portfolio;
import com.stockmatch.portfolio.domain.PortfolioDailySummary;
import com.stockmatch.portfolio.dto.PortfolioValuationResponse;
import com.stockmatch.portfolio.repository.PortfolioDailySummaryRepository;
import com.stockmatch.portfolio.repository.PortfolioRepository;
import com.stockmatch.portfolio.service.PortfolioValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioDailySummaryRepository dailySummaryRepository;
    private final PortfolioValuationService valuationService;

    /**
     * 매일 밤 23시 50분에 모든 포트폴리오의 오늘 자산 상태 기록
     */
    @Scheduled(cron = "0 50 23 * * ?")
    @Transactional
    public void captureDailyPortfolioSnapshots() {
        log.info("일별 포트폴리오 자산 스냅샷 기록 시작");
        LocalDate today = LocalDate.now();

        // 모든 포트폴리오 가져오기
        List<Portfolio> allPortfolios = portfolioRepository.findAll();

        int successCount = 0;

        for (Portfolio portfolio : allPortfolios) {
            try {
                PortfolioValuationResponse currentValuation = valuationService.calculate(portfolio.getId());

                PortfolioDailySummary dailySummary = PortfolioDailySummary.builder()
                        .portfolio(portfolio)
                        .date(today)
                        .totalInvested(currentValuation.totalInvested())
                        .totalValue(currentValuation.totalValue())
                        .totalPnl(currentValuation.totalPnlAmount())
                        .totalRate(currentValuation.totalPnlRate())
                        .realizedPnl(portfolio.getRealizedPnl())
                        .build();

                dailySummaryRepository.save(dailySummary);
                successCount++;

            } catch (Exception e) {
                log.error("포트폴리오 ID {} 스냅샷 생성 실패: {}", portfolio.getId(), e.getMessage());
            }
        }

        log.info("일별 포트폴리오 스냅샷 완료. 총 {}건 중 {}건 저장 성공.", allPortfolios.size(), successCount);
    }
}
