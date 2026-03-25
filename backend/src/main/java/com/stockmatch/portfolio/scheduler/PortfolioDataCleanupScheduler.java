package com.stockmatch.portfolio.scheduler;

import com.stockmatch.portfolio.repository.PortfolioDailySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioDataCleanupScheduler {

    private final PortfolioDailySummaryRepository dailySummaryRepository;

    /**
     * 매월 1일 새벽 3시 자동 실행
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    @Transactional
    public void cleanupOldPortfolioData() {
        log.info("오래된 포트폴리오 일별 데이터 삭제 배치를 시작합니다.");

        LocalDate oneYearAgo = LocalDate.now().minusYears(1);

        int deletedCount = dailySummaryRepository.deleteOldDailyDataLeavingOnlyLastDayOfMonth(oneYearAgo);

        log.info("포트폴리오 데이터 압축 완료. 기준일: {}, 삭제된 데이터 수: {}건", oneYearAgo, deletedCount);
    }
}
